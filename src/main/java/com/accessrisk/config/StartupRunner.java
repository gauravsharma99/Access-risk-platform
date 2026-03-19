package com.accessrisk.config;

import com.accessrisk.dto.response.RiskViolationResponse;
import com.accessrisk.enums.Severity;
import com.accessrisk.enums.ViolationStatus;
import com.accessrisk.repository.*;
import com.accessrisk.service.RiskAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs immediately after DataSeeder (@Order(1)).
 *
 * Responsibilities:
 *   1. Trigger the initial risk analysis so violations are populated at startup.
 *   2. Print a startup banner summarising the system state — useful during
 *      development to confirm seed + analysis ran correctly without opening Swagger.
 *
 * Idempotency: if violations already exist (e.g. app restart), analysis is skipped
 * to avoid duplicating OPEN violations — the service itself guards against duplicates,
 * but skipping here avoids unnecessary DB queries on every restart.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class StartupRunner implements CommandLineRunner {

    private final RiskAnalysisService    riskAnalysisService;
    private final UserRepository         userRepository;
    private final RoleRepository         roleRepository;
    private final PermissionRepository   permissionRepository;
    private final RiskRuleRepository     riskRuleRepository;
    private final RiskViolationRepository riskViolationRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("[StartupRunner] No users found — skipping analysis.");
            return;
        }

        // Run initial analysis only if no violations exist yet
        // (service is idempotent, but this avoids noise on restarts)
        long existingViolations = riskViolationRepository.count();
        List<RiskViolationResponse> newViolations;

        if (existingViolations == 0) {
            log.info("[StartupRunner] Running initial risk analysis...");
            newViolations = riskAnalysisService.analyzeAll();
            log.info("[StartupRunner] Analysis complete — {} violation(s) detected.", newViolations.size());
        } else {
            log.info("[StartupRunner] Violations already present — skipping re-analysis.");
            newViolations = riskAnalysisService.getAllViolations();
        }

        printBanner(newViolations);
    }

    // =========================================================================
    // Startup banner
    // =========================================================================

    private void printBanner(List<RiskViolationResponse> violations) {
        long users       = userRepository.count();
        long roles       = roleRepository.count();
        long permissions = permissionRepository.count();
        long activeRules = riskRuleRepository.countByActiveTrue();
        long openCount   = violations.stream()
                .filter(v -> v.getStatus() == ViolationStatus.OPEN).count();

        Map<Severity, Long> bySeverity = violations.stream()
                .collect(Collectors.groupingBy(RiskViolationResponse::getSeverity,
                         Collectors.counting()));

        // Collect per-user violation summary lines
        Map<String, Long> byUser = violations.stream()
                .collect(Collectors.groupingBy(RiskViolationResponse::getUserName,
                         Collectors.counting()));

        String userLines = byUser.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> String.format("    %-30s %d violation(s)", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));

        String violationLines = violations.stream()
                .map(v -> String.format("    [%-8s] %-18s  %s",
                        v.getSeverity(), v.getUserName(), v.getRuleName()))
                .collect(Collectors.joining("\n"));

        log.info("""

        ╔══════════════════════════════════════════════════════════════════╗
        ║          ACCESS RISK MONITORING PLATFORM  —  READY               ║
        ╠══════════════════════════════════════════════════════════════════╣
        ║  SYSTEM STATE                                                     ║
        ║    Users          : {}                                            ║
        ║    Roles          : {}                                            ║
        ║    Permissions    : {}                                            ║
        ║    Active Rules   : {}                                            ║
        ╠══════════════════════════════════════════════════════════════════╣
        ║  RISK ANALYSIS RESULTS                                            ║
        ║    Open Violations: {}                                            ║
        ║    CRITICAL       : {}                                            ║
        ║    HIGH           : {}                                            ║
        ║    MEDIUM         : {}                                            ║
        ║    LOW            : {}                                            ║
        ╠══════════════════════════════════════════════════════════════════╣
        ║  VIOLATIONS BY USER                                               ║
        {}
        ╠══════════════════════════════════════════════════════════════════╣
        ║  VIOLATION DETAIL                                                 ║
        {}
        ╠══════════════════════════════════════════════════════════════════╣
        ║  API ENDPOINTS                                                    ║
        ║    Swagger UI     : http://localhost:8080/swagger-ui.html         ║
        ║    API Docs       : http://localhost:8080/api-docs                ║
        ║    Dashboard      : GET  /api/dashboard/summary                   ║
        ║    All Violations : GET  /api/risk/violations                     ║
        ║    Re-Analyze     : POST /api/risk/analyze                        ║
        ║    Audit Log      : GET  /api/audit-logs                          ║
        ╚══════════════════════════════════════════════════════════════════╝
        """,
                users, roles, permissions, activeRules,
                openCount,
                bySeverity.getOrDefault(Severity.CRITICAL, 0L),
                bySeverity.getOrDefault(Severity.HIGH,     0L),
                bySeverity.getOrDefault(Severity.MEDIUM,   0L),
                bySeverity.getOrDefault(Severity.LOW,      0L),
                userLines.isBlank() ? "    (none)" : userLines,
                violationLines.isBlank() ? "    (none)" : violationLines
        );
    }
}
