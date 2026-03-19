package com.accessrisk.service;

import com.accessrisk.dto.request.UserRequest;
import com.accessrisk.dto.response.UserResponse;
import com.accessrisk.entity.User;
import com.accessrisk.enums.AuditAction;
import com.accessrisk.enums.ErrorCode;
import com.accessrisk.exception.DuplicateResourceException;
import com.accessrisk.exception.ResourceNotFoundException;
import com.accessrisk.repository.UserRepository;
import com.accessrisk.util.AuditDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "A user with email '" + request.getEmail() + "' already exists",
                    ErrorCode.DUPLICATE_EMAIL
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .department(request.getDepartment())
                .build();

        User saved = userRepository.save(user);

        auditLogService.log(
                AuditAction.USER_CREATED,
                "User",
                saved.getId(),
                AuditDetails.of(
                        "name", saved.getName(),
                        "email", saved.getEmail(),
                        "department", saved.getDepartment()
                )
        );

        log.info("User created: id={} email={}", saved.getId(), saved.getEmail());
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = findOrThrow(id);

        // Allow updating to the same email (idempotent), but block taking another user's email
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email '" + request.getEmail() + "' is already in use by another user",
                    ErrorCode.DUPLICATE_EMAIL
            );
        }

        // Capture previous state for the audit trail before mutating
        String previousDetails = AuditDetails.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "department", user.getDepartment()
        );

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());

        User saved = userRepository.save(user);

        auditLogService.log(
                AuditAction.USER_UPDATED,
                "User",
                saved.getId(),
                AuditDetails.of(
                        "before", previousDetails,
                        "after", AuditDetails.of(
                                "name", saved.getName(),
                                "email", saved.getEmail(),
                                "department", saved.getDepartment()
                        )
                )
        );

        return UserResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        User user = findOrThrow(id);
        userRepository.delete(user);

        auditLogService.log(
                AuditAction.USER_DELETED,
                "User",
                id,
                AuditDetails.of(
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "department", user.getDepartment()
                )
        );

        log.info("User deleted: id={} email={}", id, user.getEmail());
    }

    // -------------------------------------------------------------------------
    // Package-visible — used by AssignmentService and RiskAnalysisService
    // -------------------------------------------------------------------------

    User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
