package com.accessrisk.service;

import com.accessrisk.dto.request.RiskRuleRequest;
import com.accessrisk.dto.response.RiskRuleResponse;
import com.accessrisk.entity.Permission;
import com.accessrisk.entity.RiskRule;
import com.accessrisk.enums.AuditAction;
import com.accessrisk.enums.ErrorCode;
import com.accessrisk.exception.BusinessException;
import com.accessrisk.exception.ResourceNotFoundException;
import com.accessrisk.repository.RiskRuleRepository;
import com.accessrisk.util.AuditDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskRuleService {

    private final RiskRuleRepository riskRuleRepository;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<RiskRuleResponse> getAll() {
        return riskRuleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RiskRuleResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public RiskRuleResponse create(RiskRuleRequest request) {
        if (request.getPermissionAId().equals(request.getPermissionBId())) {
            throw new BusinessException(
                    "A risk rule must reference two different permissions — permissionAId and permissionBId cannot be equal",
                    ErrorCode.SELF_REFERENCING_RULE
            );
        }

        // Validate both permissions exist
        Permission permA = permissionService.findOrThrow(request.getPermissionAId());
        Permission permB = permissionService.findOrThrow(request.getPermissionBId());

        RiskRule rule = RiskRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissionAId(request.getPermissionAId())
                .permissionBId(request.getPermissionBId())
                .severity(request.getSeverity())
                .active(true)
                .build();

        RiskRule saved = riskRuleRepository.save(rule);

        auditLogService.log(
                AuditAction.RISK_RULE_CREATED,
                "RiskRule",
                saved.getId(),
                AuditDetails.of(
                        "name", saved.getName(),
                        "permissionA", permA.getName(),
                        "permissionB", permB.getName(),
                        "severity", saved.getSeverity(),
                        "active", saved.isActive()
                )
        );

        log.info("Risk rule created: id={} name={} [{} + {}] severity={}",
                saved.getId(), saved.getName(), permA.getName(), permB.getName(), saved.getSeverity());

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Package-visible — used by RiskAnalysisService
    // -------------------------------------------------------------------------

    RiskRule findOrThrow(Long id) {
        return riskRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RiskRule", id));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private RiskRuleResponse toResponse(RiskRule rule) {
        Permission permA = permissionService.findOrThrow(rule.getPermissionAId());
        Permission permB = permissionService.findOrThrow(rule.getPermissionBId());
        return RiskRuleResponse.from(rule, permA.getName(), permB.getName());
    }
}
