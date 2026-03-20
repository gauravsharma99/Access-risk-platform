package com.accessrisk.service;

import com.accessrisk.dto.response.RiskViolationResponse;
import com.accessrisk.entity.*;
import com.accessrisk.enums.AuditAction;
import com.accessrisk.enums.Severity;
import com.accessrisk.enums.ViolationStatus;
import com.accessrisk.exception.ResourceNotFoundException;
import com.accessrisk.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskAnalysisService")
class RiskAnalysisServiceTest {

    @Mock private UserRepository             userRepository;
    @Mock private RolePermissionRepository   rolePermissionRepository;
    @Mock private RiskRuleRepository         riskRuleRepository;
    @Mock private RiskViolationRepository    riskViolationRepository;
    @Mock private PermissionRepository       permissionRepository;
    @Mock private AuditLogService            auditLogService;

    @InjectMocks private RiskAnalysisService riskAnalysisService;

    // =========================================================================
    // Test 1 — No active rules means nothing to evaluate
    // =========================================================================

    @Test
    @DisplayName("analyzeAll: no active risk rules returns empty list without touching violation table")
    void analyzeAll_noActiveRules_returnsEmptyListAndSavesNoViolations() {
        // given
        given(riskRuleRepository.findByActiveTrue()).willReturn(List.of());

        // when
        List<RiskViolationResponse> result = riskAnalysisService.analyzeAll();

        // then
        assertThat(result).isEmpty();
        then(riskViolationRepository).should(never()).save(any());
    }

    // =========================================================================
    // Test 2 — User with both conflicting permissions triggers a violation
    // =========================================================================

    @Test
    @DisplayName("analyzeUser: user holding both conflicting permissions creates a violation with explanation")
    void analyzeUser_userHasBothConflictingPermissions_createsViolationWithExplanation() {
        // given — user Carol has effective permissions that match the rule
        User carol = buildUser(1L, "Carol Martinez", "carol@company.com", "Finance");
        Permission createVendor  = buildPermission(10L, "CREATE_VENDOR");
        Permission approvePayment = buildPermission(20L, "APPROVE_PAYMENT");

        RiskRule rule = buildRule(5L, "Vendor-Payment SoD",
                "financial approval fraud risk", 10L, 20L, Severity.CRITICAL);

        given(userRepository.findById(1L)).willReturn(Optional.of(carol));
        given(riskRuleRepository.findByActiveTrue()).willReturn(List.of(rule));

        // Carol's effective permissions include both sides of the rule
        given(rolePermissionRepository.findEffectivePermissionIdsByUserId(1L))
                .willReturn(Set.of(10L, 20L));

        // No pre-existing open violation for this user+rule
        given(riskViolationRepository.findByUserIdAndRuleIdAndStatus(1L, 5L, ViolationStatus.OPEN))
                .willReturn(Optional.empty());

        given(permissionRepository.findAllById(Set.of(10L, 20L)))
                .willReturn(List.of(createVendor, approvePayment));

        // Capture the saved violation so we can assert on it
        given(riskViolationRepository.save(any(RiskViolation.class))).willAnswer(inv -> {
            RiskViolation v = inv.getArgument(0);
            v.setId(100L);
            return v;
        });

        // when
        List<RiskViolationResponse> result = riskAnalysisService.analyzeUser(1L);

        // then — exactly one violation created
        assertThat(result).hasSize(1);

        RiskViolationResponse violation = result.get(0);
        assertThat(violation.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(violation.getStatus()).isEqualTo(ViolationStatus.OPEN);
        assertThat(violation.getUserName()).isEqualTo("Carol Martinez");
        assertThat(violation.getRuleName()).isEqualTo("Vendor-Payment SoD");

        // Explanation must name both conflicting permissions
        assertThat(violation.getExplanation())
                .contains("CREATE_VENDOR")
                .contains("APPROVE_PAYMENT")
                .contains("Carol Martinez");

        // Audit entry must be written for the detected violation
        then(auditLogService).should()
                .log(eq(AuditAction.VIOLATION_DETECTED), eq("RiskViolation"), eq(100L), anyString());
    }

    // =========================================================================
    // Test 3 — User with only one side of the conflict is NOT flagged
    // =========================================================================

    @Test
    @DisplayName("analyzeUser: user missing one conflicting permission produces no violation")
    void analyzeUser_userHasOnlyOneConflictingPermission_noViolationCreated() {
        // given — Alice only has CREATE_VENDOR (10), not APPROVE_PAYMENT (20)
        User alice = buildUser(2L, "Alice Johnson", "alice@company.com", "Finance");
        RiskRule rule = buildRule(5L, "Vendor-Payment SoD",
                "financial fraud risk", 10L, 20L, Severity.CRITICAL);

        given(userRepository.findById(2L)).willReturn(Optional.of(alice));
        given(riskRuleRepository.findByActiveTrue()).willReturn(List.of(rule));
        given(rolePermissionRepository.findEffectivePermissionIdsByUserId(2L))
                .willReturn(Set.of(10L));   // only CREATE_VENDOR

        given(permissionRepository.findAllById(Set.of(10L, 20L)))
                .willReturn(List.of(buildPermission(10L, "CREATE_VENDOR"),
                                    buildPermission(20L, "APPROVE_PAYMENT")));

        // when
        List<RiskViolationResponse> result = riskAnalysisService.analyzeUser(2L);

        // then
        assertThat(result).isEmpty();
        then(riskViolationRepository).should(never()).save(any());
    }

    // =========================================================================
    // Test 4 — Re-running analysis does not duplicate an existing OPEN violation
    // =========================================================================

    @Test
    @DisplayName("analyzeUser: existing OPEN violation for same user+rule is not duplicated")
    void analyzeUser_openViolationAlreadyExists_skipsAndCreatesNoDuplicate() {
        // given
        User carol = buildUser(1L, "Carol Martinez", "carol@company.com", "Finance");
        RiskRule rule = buildRule(5L, "Vendor-Payment SoD",
                "financial fraud risk", 10L, 20L, Severity.CRITICAL);

        given(userRepository.findById(1L)).willReturn(Optional.of(carol));
        given(riskRuleRepository.findByActiveTrue()).willReturn(List.of(rule));
        given(rolePermissionRepository.findEffectivePermissionIdsByUserId(1L))
                .willReturn(Set.of(10L, 20L));

        given(permissionRepository.findAllById(Set.of(10L, 20L)))
                .willReturn(List.of(buildPermission(10L, "CREATE_VENDOR"),
                                    buildPermission(20L, "APPROVE_PAYMENT")));

        // Simulate an already-open violation for this user+rule
        given(riskViolationRepository.findByUserIdAndRuleIdAndStatus(1L, 5L, ViolationStatus.OPEN))
                .willReturn(Optional.of(new RiskViolation()));

        // when
        List<RiskViolationResponse> result = riskAnalysisService.analyzeUser(1L);

        // then — no new violation is written
        assertThat(result).isEmpty();
        then(riskViolationRepository).should(never()).save(any());
    }

    // =========================================================================
    // Test 5 — Full-system scan: only the risky user accumulates violations
    // =========================================================================

    @Test
    @DisplayName("analyzeAll: clean user produces no violations; risky user produces one")
    void analyzeAll_oneCleanAndOneRiskyUser_createsViolationOnlyForRiskyUser() {
        // given — two users, one clean, one with conflicting permissions
        User alice = buildUser(1L, "Alice Johnson", "alice@company.com", "Finance");
        User carol = buildUser(2L, "Carol Martinez", "carol@company.com", "Finance");

        RiskRule rule = buildRule(5L, "Vendor-Payment SoD",
                "financial fraud risk", 10L, 20L, Severity.CRITICAL);

        given(riskRuleRepository.findByActiveTrue()).willReturn(List.of(rule));
        given(userRepository.findAll()).willReturn(List.of(alice, carol));

        given(permissionRepository.findAllById(Set.of(10L, 20L)))
                .willReturn(List.of(buildPermission(10L, "CREATE_VENDOR"),
                                    buildPermission(20L, "APPROVE_PAYMENT")));

        // Alice has only CREATE_VENDOR — clean
        given(rolePermissionRepository.findEffectivePermissionIdsByUserId(1L))
                .willReturn(Set.of(10L));

        // Carol has both — risky
        given(rolePermissionRepository.findEffectivePermissionIdsByUserId(2L))
                .willReturn(Set.of(10L, 20L));

        given(riskViolationRepository.findByUserIdAndRuleIdAndStatus(2L, 5L, ViolationStatus.OPEN))
                .willReturn(Optional.empty());

        given(riskViolationRepository.save(any(RiskViolation.class))).willAnswer(inv -> {
            RiskViolation v = inv.getArgument(0);
            v.setId(99L);
            return v;
        });

        // when
        List<RiskViolationResponse> result = riskAnalysisService.analyzeAll();

        // then — exactly one violation, belonging to Carol
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserName()).isEqualTo("Carol Martinez");
        assertThat(result.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);

        // Confirm the violation was persisted once (for Carol only)
        ArgumentCaptor<RiskViolation> captor = ArgumentCaptor.forClass(RiskViolation.class);
        then(riskViolationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(2L);
        assertThat(captor.getValue().getRuleId()).isEqualTo(5L);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User buildUser(Long id, String name, String email, String dept) {
        User u = User.builder().name(name).email(email).department(dept).build();
        u.setId(id);
        return u;
    }

    private Permission buildPermission(Long id, String name) {
        Permission p = Permission.builder().name(name).description(name).build();
        p.setId(id);
        return p;
    }

    private RiskRule buildRule(Long id, String name, String description,
                               Long permAId, Long permBId, Severity severity) {
        RiskRule r = RiskRule.builder()
                .name(name)
                .description(description)
                .permissionAId(permAId)
                .permissionBId(permBId)
                .severity(severity)
                .active(true)
                .build();
        r.setId(id);
        return r;
    }
}
