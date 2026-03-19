package com.accessrisk.service;

import com.accessrisk.dto.response.RiskViolationResponse;
import com.accessrisk.entity.Permission;
import com.accessrisk.entity.RiskRule;
import com.accessrisk.entity.RiskViolation;
import com.accessrisk.entity.User;
import com.accessrisk.enums.AuditAction;
import com.accessrisk.enums.ViolationStatus;
import com.accessrisk.exception.ResourceNotFoundException;
import com.accessrisk.repository.PermissionRepository;
import com.accessrisk.repository.RiskRuleRepository;
import com.accessrisk.repository.RiskViolationRepository;
import com.accessrisk.repository.UserRepository;
import com.accessrisk.repository.RolePermissionRepository;
import com.accessrisk.util.AuditDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAnalysisService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RiskRuleRepository riskRuleRepository;
    private final RiskViolationRepository riskViolationRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogService auditLogService;

    /**
     * Runs risk analysis across all users in the system.
     *
     * Algorithm:
     *  1. Load all active risk rules once — shared across all user iterations.
     *  2. Build a permission name lookup map once — prevents N+1 queries inside the loop.
     *  3. For each user, compute their effective permission set via a single JPQL query.
     *  4. For each active rule, check if the user holds both conflicting permissions.
     *  5. Guard against duplicates: skip if an OPEN violation already exists for this user+rule.
     *  6. Persist new violations and emit a VIOLATION_DETECTED audit entry for each.
     *
     * @return newly created violations from this run (does NOT include pre-existing ones)
     */
    @Transactional
    public List<RiskViolationResponse> analyzeAll() {
        List<RiskRule> activeRules = riskRuleRepository.findByActiveTrue();

        if (activeRules.isEmpty()) {
            log.info("Risk analysis skipped — no active rules configured");
            return List.of();
        }

        auditLogService.log(
                AuditAction.RISK_ANALYSIS_STARTED,
                "System",
                null,
                AuditDetails.of("activeRules", activeRules.size(), "scope", "ALL_USERS")
        );

        Map<Long, String> permissionNameById = buildPermissionNameCache(activeRules);
        List<User> users = userRepository.findAll();
        List<RiskViolationResponse> newViolations = new ArrayList<>();

        for (User user : users) {
            newViolations.addAll(analyzeUserInternal(user, activeRules, permissionNameById));
        }

        auditLogService.log(
                AuditAction.RISK_ANALYSIS_COMPLETED,
                "System",
                null,
                AuditDetails.of(
                        "usersScanned", users.size(),
                        "activeRules", activeRules.size(),
                        "newViolations", newViolations.size()
                )
        );

        log.info("Risk analysis complete — {} new violation(s) across {} user(s)",
                newViolations.size(), users.size());

        return newViolations;
    }

    /**
     * Runs risk analysis for a single user.
     * Useful for on-demand checks immediately after a new role is assigned.
     */
    @Transactional
    public List<RiskViolationResponse> analyzeUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<RiskRule> activeRules = riskRuleRepository.findByActiveTrue();
        Map<Long, String> permissionNameById = buildPermissionNameCache(activeRules);

        auditLogService.log(
                AuditAction.RISK_ANALYSIS_STARTED,
                "User",
                userId,
                AuditDetails.of(
                        "userName", user.getName(),
                        "activeRules", activeRules.size(),
                        "scope", "SINGLE_USER"
                )
        );

        List<RiskViolationResponse> violations =
                analyzeUserInternal(user, activeRules, permissionNameById);

        auditLogService.log(
                AuditAction.RISK_ANALYSIS_COMPLETED,
                "User",
                userId,
                AuditDetails.of(
                        "userName", user.getName(),
                        "newViolations", violations.size()
                )
        );

        return violations;
    }

    /**
     * Returns all recorded violations with enriched user and rule names.
     */
    @Transactional(readOnly = true)
    public List<RiskViolationResponse> getAllViolations() {
        return riskViolationRepository.findAll()
                .stream()
                .map(this::toEnrichedResponse)
                .toList();
    }

    /**
     * Returns violations filtered by status.
     */
    @Transactional(readOnly = true)
    public List<RiskViolationResponse> getViolationsByStatus(ViolationStatus status) {
        return riskViolationRepository.findByStatus(status)
                .stream()
                .map(this::toEnrichedResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Core analysis logic
    // -------------------------------------------------------------------------

    private List<RiskViolationResponse> analyzeUserInternal(
            User user,
            List<RiskRule> activeRules,
            Map<Long, String> permissionNameById) {

        Set<Long> effectivePermissions =
                rolePermissionRepository.findEffectivePermissionIdsByUserId(user.getId());

        if (effectivePermissions.isEmpty()) {
            return List.of();
        }

        List<RiskViolationResponse> created = new ArrayList<>();

        for (RiskRule rule : activeRules) {
            boolean hasPermA = effectivePermissions.contains(rule.getPermissionAId());
            boolean hasPermB = effectivePermissions.contains(rule.getPermissionBId());

            if (!hasPermA || !hasPermB) {
                continue;
            }

            // Idempotency: skip if this user+rule already has an OPEN violation
            boolean alreadyOpen = riskViolationRepository
                    .findByUserIdAndRuleIdAndStatus(user.getId(), rule.getId(), ViolationStatus.OPEN)
                    .isPresent();

            if (alreadyOpen) {
                log.debug("Skipping duplicate violation — userId={} ruleId={}", user.getId(), rule.getId());
                continue;
            }

            String permAName = permissionNameById.getOrDefault(rule.getPermissionAId(), "UNKNOWN");
            String permBName = permissionNameById.getOrDefault(rule.getPermissionBId(), "UNKNOWN");
            String explanation = buildExplanation(user.getName(), permAName, permBName, rule);

            RiskViolation violation = RiskViolation.builder()
                    .userId(user.getId())
                    .ruleId(rule.getId())
                    .severity(rule.getSeverity())
                    .status(ViolationStatus.OPEN)
                    .explanation(explanation)
                    .build();

            RiskViolation saved = riskViolationRepository.save(violation);

            auditLogService.log(
                    AuditAction.VIOLATION_DETECTED,
                    "RiskViolation",
                    saved.getId(),
                    AuditDetails.of(
                            "userId", user.getId(),
                            "userName", user.getName(),
                            "ruleId", rule.getId(),
                            "ruleName", rule.getName(),
                            "permissionA", permAName,
                            "permissionB", permBName,
                            "severity", rule.getSeverity()
                    )
            );

            log.warn("Violation detected — userId={} userName={} ruleId={} ruleName={} severity={}",
                    user.getId(), user.getName(), rule.getId(), rule.getName(), rule.getSeverity());

            created.add(RiskViolationResponse.from(saved, user.getName(), rule.getName()));
        }

        return created;
    }

    /**
     * Generates a human-readable explanation in the SAP IAG SoD style.
     *
     * Example:
     * "User 'Alice Smith' has conflicting permissions 'CREATE_VENDOR' and 'APPROVE_PAYMENT'
     *  which creates financial approval fraud risk. Rule: Vendor-Payment SoD (Severity: CRITICAL)"
     */
    private String buildExplanation(String userName, String permAName, String permBName, RiskRule rule) {
        String riskDescription = (rule.getDescription() != null && !rule.getDescription().isBlank())
                ? rule.getDescription()
                : "a segregation of duties conflict";

        return String.format(
                "User '%s' has conflicting permissions '%s' and '%s' which creates %s. " +
                "Rule: %s (Severity: %s)",
                userName, permAName, permBName, riskDescription, rule.getName(), rule.getSeverity()
        );
    }

    /**
     * Pre-loads all permission names referenced by the active rules into a Map.
     * Prevents repeated individual DB lookups inside the nested user+rule loop.
     */
    private Map<Long, String> buildPermissionNameCache(List<RiskRule> rules) {
        Set<Long> permissionIds = rules.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getPermissionAId(), r.getPermissionBId()))
                .collect(Collectors.toSet());

        return permissionRepository.findAllById(permissionIds)
                .stream()
                .collect(Collectors.toMap(Permission::getId, Permission::getName));
    }

    // -------------------------------------------------------------------------
    // Response enrichment — resolves IDs to names for the API response
    // -------------------------------------------------------------------------

    private RiskViolationResponse toEnrichedResponse(RiskViolation violation) {
        String userName = userRepository.findById(violation.getUserId())
                .map(User::getName)
                .orElse("[deleted user]");

        String ruleName = riskRuleRepository.findById(violation.getRuleId())
                .map(RiskRule::getName)
                .orElse("[deleted rule]");

        return RiskViolationResponse.from(violation, userName, ruleName);
    }
}
