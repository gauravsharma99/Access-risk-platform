package com.accessrisk.service;

import com.accessrisk.AbstractIntegrationTest;
import com.accessrisk.dto.response.RiskViolationResponse;
import com.accessrisk.entity.*;
import com.accessrisk.enums.Severity;
import com.accessrisk.enums.ViolationStatus;
import com.accessrisk.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RiskAnalysisService against a real PostgreSQL database.
 *
 * The DataSeeder and StartupRunner fire on application startup, so the database
 * already contains 4 users, 4 roles, 8 permissions, 4 risk rules and 6 OPEN
 * violations when these tests run.
 *
 * Each test is @Transactional — service methods join the outer test transaction,
 * and all writes are rolled back after each test. Seed data survives across tests.
 */
@Transactional
@DisplayName("RiskAnalysisService — integration")
class RiskAnalysisIntegrationTest extends AbstractIntegrationTest {

    @Autowired private RiskAnalysisService riskAnalysisService;

    @Autowired private UserRepository         userRepository;
    @Autowired private RoleRepository         roleRepository;
    @Autowired private PermissionRepository   permissionRepository;
    @Autowired private UserRoleRepository     userRoleRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private RiskViolationRepository riskViolationRepository;

    // =========================================================================
    // Test 1 — System-level idempotency
    // =========================================================================

    @Test
    @DisplayName("analyzeAll: re-running when all violations are already OPEN creates no duplicates")
    void analyzeAll_allViolationsAlreadyOpen_returnsNoNewViolations() {
        // Seed data + StartupRunner already created 6 OPEN violations.
        // Running again must return empty — the idempotency guard must fire for all 6.

        List<RiskViolationResponse> newViolations = riskAnalysisService.analyzeAll();

        assertThat(newViolations).isEmpty();
        // Total violation count in the DB must still be exactly 6
        assertThat(riskViolationRepository.findAll()).hasSize(6);
    }

    // =========================================================================
    // Test 2 — Clean user: no roles means no effective permissions
    // =========================================================================

    @Test
    @DisplayName("analyzeUser: user with no role assignments produces no violations")
    void analyzeUser_userWithNoRoles_producesNoViolations() {
        User freshUser = userRepository.save(
                User.builder()
                        .name("Test User Clean")
                        .email("clean.test@company.com")
                        .department("Testing")
                        .build()
        );

        List<RiskViolationResponse> violations = riskAnalysisService.analyzeUser(freshUser.getId());

        assertThat(violations).isEmpty();
    }

    // =========================================================================
    // Test 3 — Risky user: conflicting permissions trigger a CRITICAL violation
    // =========================================================================

    @Test
    @DisplayName("analyzeUser: user with CREATE_VENDOR + PROCESS_PAYMENT triggers CRITICAL Vendor-Payment SoD violation")
    void analyzeUser_userWithConflictingPermissions_detectsCriticalViolation() {
        // Use seeded permissions and risk rule — look them up by name
        Permission createVendor = permissionRepository.findByName("CREATE_VENDOR")
                .orElseThrow(() -> new IllegalStateException("Seed permission CREATE_VENDOR not found"));
        Permission processPayment = permissionRepository.findByName("PROCESS_PAYMENT")
                .orElseThrow(() -> new IllegalStateException("Seed permission PROCESS_PAYMENT not found"));

        // Create a dedicated test role and assign both conflicting permissions to it
        Role testRole = roleRepository.save(
                Role.builder().name("TEST_RISKY_ROLE").description("Integration test role").build()
        );
        rolePermissionRepository.save(
                RolePermission.builder().roleId(testRole.getId()).permissionId(createVendor.getId()).build()
        );
        rolePermissionRepository.save(
                RolePermission.builder().roleId(testRole.getId()).permissionId(processPayment.getId()).build()
        );

        // Create a test user and assign the risky role
        User riskyUser = userRepository.save(
                User.builder()
                        .name("Test User Risky")
                        .email("risky.test@company.com")
                        .department("Testing")
                        .build()
        );
        userRoleRepository.save(
                UserRole.builder().userId(riskyUser.getId()).roleId(testRole.getId()).build()
        );

        // Run analysis for this user
        List<RiskViolationResponse> violations = riskAnalysisService.analyzeUser(riskyUser.getId());

        // The Vendor-Payment SoD rule (CRITICAL) must fire
        assertThat(violations).hasSize(1);

        RiskViolationResponse violation = violations.get(0);
        assertThat(violation.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(violation.getStatus()).isEqualTo(ViolationStatus.OPEN);
        assertThat(violation.getUserName()).isEqualTo("Test User Risky");
        assertThat(violation.getRuleName()).isEqualTo("Vendor-Payment SoD");
        assertThat(violation.getExplanation())
                .contains("CREATE_VENDOR")
                .contains("PROCESS_PAYMENT")
                .contains("Test User Risky");
    }
}
