package com.accessrisk.service;

import com.accessrisk.dto.request.UserRequest;
import com.accessrisk.dto.response.UserResponse;
import com.accessrisk.entity.User;
import com.accessrisk.enums.AuditAction;
import com.accessrisk.exception.DuplicateResourceException;
import com.accessrisk.exception.ResourceNotFoundException;
import com.accessrisk.repository.UserRepository;
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
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private UserService userService;

    // =========================================================================
    // Test 1 — Happy path: create a user
    // =========================================================================

    @Test
    @DisplayName("create: valid request persists user and returns populated response")
    void create_validRequest_savesUserAndReturnsResponse() {
        // given
        UserRequest request = userRequest("Alice Johnson", "alice@company.com", "Finance");

        given(userRepository.existsByEmail("alice@company.com")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        // when
        UserResponse response = userService.create(request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice Johnson");
        assertThat(response.getEmail()).isEqualTo("alice@company.com");
        assertThat(response.getDepartment()).isEqualTo("Finance");

        then(auditLogService).should()
                .log(eq(AuditAction.USER_CREATED), eq("User"), eq(1L), anyString());
    }

    // =========================================================================
    // Test 2 — Duplicate email is rejected before any DB write
    // =========================================================================

    @Test
    @DisplayName("create: duplicate email throws DuplicateResourceException and never saves")
    void create_emailAlreadyExists_throwsDuplicateResourceException() {
        // given
        UserRequest request = userRequest("Bob", "taken@company.com", "IT");
        given(userRepository.existsByEmail("taken@company.com")).willReturn(true);

        // when / then
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken@company.com");

        then(userRepository).should(never()).save(any());
        then(auditLogService).should(never()).log(any(), any(), any(), any());
    }

    // =========================================================================
    // Test 3 — Update cannot steal another user's email
    // =========================================================================

    @Test
    @DisplayName("update: email belonging to a different user throws DuplicateResourceException")
    void update_emailOwnedByAnotherUser_throwsDuplicateResourceException() {
        // given — Alice exists with id=1
        User alice = buildUser(1L, "Alice", "alice@company.com", "Finance");
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));

        // The request tries to change Alice's email to Bob's (already taken)
        UserRequest request = userRequest("Alice Updated", "bob@company.com", "Finance");
        given(userRepository.existsByEmail("bob@company.com")).willReturn(true);

        // when / then
        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("bob@company.com");

        then(userRepository).should(never()).save(any());
    }

    // =========================================================================
    // Test 4 — Delete writes an audit log and removes the entity
    // =========================================================================

    @Test
    @DisplayName("delete: existing user is removed and a USER_DELETED audit entry is written")
    void delete_existingUser_deletesEntityAndWritesAuditLog() {
        // given
        User alice = buildUser(1L, "Alice Johnson", "alice@company.com", "Finance");
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));

        // when
        userService.delete(1L);

        // then — entity is deleted
        then(userRepository).should().delete(alice);

        // then — audit record captures the deletion
        then(auditLogService).should()
                .log(eq(AuditAction.USER_DELETED), eq("User"), eq(1L), anyString());
    }

    // =========================================================================
    // Test 5 — getById for a missing record throws the right exception
    // =========================================================================

    @Test
    @DisplayName("getById: unknown id throws ResourceNotFoundException with the id in the message")
    void getById_unknownId_throwsResourceNotFoundException() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private UserRequest userRequest(String name, String email, String dept) {
        UserRequest r = new UserRequest();
        r.setName(name);
        r.setEmail(email);
        r.setDepartment(dept);
        return r;
    }

    private User buildUser(Long id, String name, String email, String dept) {
        User u = User.builder().name(name).email(email).department(dept).build();
        u.setId(id);
        return u;
    }
}
