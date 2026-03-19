package com.accessrisk.service;

import com.accessrisk.dto.response.DashboardSummaryResponse;
import com.accessrisk.enums.Severity;
import com.accessrisk.enums.ViolationStatus;
import com.accessrisk.repository.PermissionRepository;
import com.accessrisk.repository.RiskRuleRepository;
import com.accessrisk.repository.RiskViolationRepository;
import com.accessrisk.repository.RoleRepository;
import com.accessrisk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RiskRuleRepository riskRuleRepository;
    private final RiskViolationRepository riskViolationRepository;

    /**
     * Aggregates key platform metrics into a single response.
     *
     * All counts use Spring Data's built-in count queries — each translates to a
     * single SQL COUNT(*), so this method issues exactly 7 lightweight queries.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long openViolations = riskViolationRepository.countByStatus(ViolationStatus.OPEN);

        long criticalViolations = countBySeverity(Severity.CRITICAL);
        long highViolations     = countBySeverity(Severity.HIGH);
        long mediumViolations   = countBySeverity(Severity.MEDIUM);
        long lowViolations      = countBySeverity(Severity.LOW);

        return DashboardSummaryResponse.builder()
                .totalUsers(userRepository.count())
                .totalRoles(roleRepository.count())
                .totalPermissions(permissionRepository.count())
                .activeRiskRules(riskRuleRepository.countByActiveTrue())
                .openViolations(openViolations)
                .criticalViolations(criticalViolations)
                .highViolations(highViolations)
                .mediumViolations(mediumViolations)
                .lowViolations(lowViolations)
                .build();
    }

    private long countBySeverity(Severity severity) {
        return riskViolationRepository.countBySeverity(severity);
    }
}
