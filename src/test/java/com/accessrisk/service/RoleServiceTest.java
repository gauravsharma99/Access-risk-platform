package com.accessrisk.service;

import com.accessrisk.dto.request.RoleRequest;
import com.accessrisk.dto.response.RoleResponse;
import com.accessrisk.entity.Role;
import com.accessrisk.enums.AuditAction;
import com.accessrisk.exception.DuplicateResourceException;
import com.accessrisk.exception.ResourceNotFoundException;
import com.accessrisk.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService")
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private RoleService roleService;

    // =========================================================================
    // Test 1 — getAll returns every role mapped to a response
    // =========================================================================

    @Test
    @DisplayName("getAll: returns all roles mapped to response DTOs")
    void getAll_rolesExist_returnsAllAsDtos() {
        // given
        Role apClerk  = buildRole(1L, "AP_CLERK",        "Accounts Payable Clerk");
        Role finMgr   = buildRole(2L, "FINANCE_MANAGER", "Finance Manager");
        given(roleRepository.findAll()).willReturn(List.of(apClerk, finMgr));

        // when
        List<RoleResponse> result = roleService.getAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(RoleResponse::getName)
                .containsExactlyInAnyOrder("AP_CLERK", "FINANCE_MANAGER");
    }

    // =========================================================================
    // Test 2 — getById returns the correct role when it exists
    // =========================================================================

    @Test
    @DisplayName("getById: existing role id returns matching response")
    void getById_existingId_returnsRoleResponse() {
        // given
        Role role = buildRole(3L, "PROCUREMENT_OFFICER", "Procurement Officer");
        given(roleRepository.findById(3L)).willReturn(Optional.of(role));

        // when
        RoleResponse response = roleService.getById(3L);

        // then
        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getName()).isEqualTo("PROCUREMENT_OFFICER");
    }

    // =========================================================================
    // Test 3 — getById for a missing role throws the right exception
    // =========================================================================

    @Test
    @DisplayName("getById: unknown id throws ResourceNotFoundException")
    void getById_unknownId_throwsResourceNotFoundException() {
        // given
        given(roleRepository.findById(99L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> roleService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // =========================================================================
    // Test 4 — create succeeds and records an audit entry
    // =========================================================================

    @Test
    @DisplayName("create: new role name is persisted and a ROLE_CREATED audit entry is written")
    void create_uniqueName_savesRoleAndWritesAuditLog() {
        // given
        RoleRequest request = roleRequest("IT_ADMIN", "IT Administrator");
        given(roleRepository.existsByName("IT_ADMIN")).willReturn(false);
        given(roleRepository.save(any(Role.class))).willAnswer(inv -> {
            Role r = inv.getArgument(0);
            r.setId(4L);
            return r;
        });

        // when
        RoleResponse response = roleService.create(request);

        // then
        assertThat(response.getName()).isEqualTo("IT_ADMIN");
        assertThat(response.getDescription()).isEqualTo("IT Administrator");

        then(auditLogService).should()
                .log(eq(AuditAction.ROLE_CREATED), eq("Role"), eq(4L), anyString());
    }

    // =========================================================================
    // Test 5 — Duplicate name is rejected before any DB write
    // =========================================================================

    @Test
    @DisplayName("create: duplicate role name throws DuplicateResourceException and never saves")
    void create_duplicateName_throwsDuplicateResourceExceptionAndNeverSaves() {
        // given
        RoleRequest request = roleRequest("AP_CLERK", "Duplicate clerk role");
        given(roleRepository.existsByName("AP_CLERK")).willReturn(true);

        // when / then
        assertThatThrownBy(() -> roleService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("AP_CLERK");

        then(roleRepository).should(never()).save(any());
        then(auditLogService).should(never()).log(any(), any(), any(), any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private RoleRequest roleRequest(String name, String description) {
        RoleRequest r = new RoleRequest();
        r.setName(name);
        r.setDescription(description);
        return r;
    }

    private Role buildRole(Long id, String name, String description) {
        Role r = Role.builder().name(name).description(description).build();
        r.setId(id);
        return r;
    }
}
