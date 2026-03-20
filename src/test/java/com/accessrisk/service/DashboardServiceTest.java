package com.accessrisk.service;

import com.accessrisk.dto.response.DashboardSummaryResponse;
import com.accessrisk.enums.Severity;
import com.accessrisk.enums.ViolationStatus;
import com.accessrisk.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceTest {

    @Mock private UserRepository           userRepository;
    @Mock private RoleRepository           roleRepository;
    @Mock private PermissionRepository     permissionRepository;
    @Mock private RiskRuleRepository       riskRuleRepository;
    @Mock private RiskViolationRepository  riskViolationRepository;

    @InjectMocks private DashboardService dashboardService;

    // =========================================================================
    // Test 1 — Aggregate entity counts are pulled from their respective repos
    // =========================================================================

    @Test
    @DisplayName("getSummary: total counts reflect values from each repository")
    void getSummary_populatedSystem_returnsTotalCountsFromAllRepositories() {
        // given
        given(userRepository.count()).willReturn(4L);
        given(roleRepository.count()).willReturn(4L);
        given(permissionRepository.count()).willReturn(8L);
        given(riskRuleRepository.countByActiveTrue()).willReturn(4L);
        given(riskViolationRepository.countByStatus(ViolationStatus.OPEN)).willReturn(6L);
        given(riskViolationRepository.countBySeverity(Severity.CRITICAL)).willReturn(2L);
        given(riskViolationRepository.countBySeverity(Severity.HIGH)).willReturn(3L);
        given(riskViolationRepository.countBySeverity(Severity.MEDIUM)).willReturn(1L);
        given(riskViolationRepository.countBySeverity(Severity.LOW)).willReturn(0L);

        // when
        DashboardSummaryResponse summary = dashboardService.getSummary();

        // then
        assertThat(summary.getTotalUsers()).isEqualTo(4L);
        assertThat(summary.getTotalRoles()).isEqualTo(4L);
        assertThat(summary.getTotalPermissions()).isEqualTo(8L);
        assertThat(summary.getActiveRiskRules()).isEqualTo(4L);
    }

    // =========================================================================
    // Test 2 — Open violation count reflects current OPEN status only
    // =========================================================================

    @Test
    @DisplayName("getSummary: openViolations count reflects only OPEN status violations")
    void getSummary_withOpenViolations_returnsCorrectOpenCount() {
        // given — 3 open violations, other statuses are ignored for this counter
        given(userRepository.count()).willReturn(4L);
        given(roleRepository.count()).willReturn(4L);
        given(permissionRepository.count()).willReturn(8L);
        given(riskRuleRepository.countByActiveTrue()).willReturn(4L);
        given(riskViolationRepository.countByStatus(ViolationStatus.OPEN)).willReturn(3L);
        given(riskViolationRepository.countBySeverity(Severity.CRITICAL)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.HIGH)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.MEDIUM)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.LOW)).willReturn(0L);

        // when
        DashboardSummaryResponse summary = dashboardService.getSummary();

        // then
        assertThat(summary.getOpenViolations()).isEqualTo(3L);
    }

    // =========================================================================
    // Test 3 — Severity breakdown is reported per-level independently
    // =========================================================================

    @Test
    @DisplayName("getSummary: severity breakdown correctly maps CRITICAL, HIGH, MEDIUM, LOW counts")
    void getSummary_withViolations_returnsSeverityBreakdownPerLevel() {
        // given — distinct counts per severity level
        given(userRepository.count()).willReturn(2L);
        given(roleRepository.count()).willReturn(2L);
        given(permissionRepository.count()).willReturn(4L);
        given(riskRuleRepository.countByActiveTrue()).willReturn(2L);
        given(riskViolationRepository.countByStatus(ViolationStatus.OPEN)).willReturn(5L);
        given(riskViolationRepository.countBySeverity(Severity.CRITICAL)).willReturn(2L);
        given(riskViolationRepository.countBySeverity(Severity.HIGH)).willReturn(2L);
        given(riskViolationRepository.countBySeverity(Severity.MEDIUM)).willReturn(1L);
        given(riskViolationRepository.countBySeverity(Severity.LOW)).willReturn(0L);

        // when
        DashboardSummaryResponse summary = dashboardService.getSummary();

        // then — each severity band is reported independently
        assertThat(summary.getCriticalViolations()).isEqualTo(2L);
        assertThat(summary.getHighViolations()).isEqualTo(2L);
        assertThat(summary.getMediumViolations()).isEqualTo(1L);
        assertThat(summary.getLowViolations()).isEqualTo(0L);
    }

    // =========================================================================
    // Test 4 — Inactive rules are excluded from the active rule count
    // =========================================================================

    @Test
    @DisplayName("getSummary: activeRiskRules excludes disabled rules")
    void getSummary_withMixOfActiveAndInactiveRules_countsOnlyActiveRules() {
        // given — 4 rules exist in total but only 2 are active
        given(userRepository.count()).willReturn(4L);
        given(roleRepository.count()).willReturn(4L);
        given(permissionRepository.count()).willReturn(8L);
        given(riskRuleRepository.countByActiveTrue()).willReturn(2L); // only 2 of 4 are active
        given(riskViolationRepository.countByStatus(ViolationStatus.OPEN)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.CRITICAL)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.HIGH)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.MEDIUM)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.LOW)).willReturn(0L);

        // when
        DashboardSummaryResponse summary = dashboardService.getSummary();

        // then — reflects active rules only, not total rule count
        assertThat(summary.getActiveRiskRules()).isEqualTo(2L);
    }

    // =========================================================================
    // Test 5 — Empty system: all counts are zero, no NullPointerException
    // =========================================================================

    @Test
    @DisplayName("getSummary: empty system returns all zero counts without errors")
    void getSummary_emptySystem_returnsAllZeroCountsSafely() {
        // given — brand new system with nothing seeded
        given(userRepository.count()).willReturn(0L);
        given(roleRepository.count()).willReturn(0L);
        given(permissionRepository.count()).willReturn(0L);
        given(riskRuleRepository.countByActiveTrue()).willReturn(0L);
        given(riskViolationRepository.countByStatus(ViolationStatus.OPEN)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.CRITICAL)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.HIGH)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.MEDIUM)).willReturn(0L);
        given(riskViolationRepository.countBySeverity(Severity.LOW)).willReturn(0L);

        // when
        DashboardSummaryResponse summary = dashboardService.getSummary();

        // then — no NPE, all fields are zero
        assertThat(summary.getTotalUsers()).isZero();
        assertThat(summary.getTotalRoles()).isZero();
        assertThat(summary.getTotalPermissions()).isZero();
        assertThat(summary.getActiveRiskRules()).isZero();
        assertThat(summary.getOpenViolations()).isZero();
        assertThat(summary.getCriticalViolations()).isZero();
        assertThat(summary.getHighViolations()).isZero();
        assertThat(summary.getMediumViolations()).isZero();
        assertThat(summary.getLowViolations()).isZero();
    }
}
