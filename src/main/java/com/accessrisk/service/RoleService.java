package com.accessrisk.service;

import com.accessrisk.dto.request.RoleRequest;
import com.accessrisk.dto.response.RoleResponse;
import com.accessrisk.entity.Role;
import com.accessrisk.enums.AuditAction;
import com.accessrisk.enums.ErrorCode;
import com.accessrisk.exception.DuplicateResourceException;
import com.accessrisk.exception.ResourceNotFoundException;
import com.accessrisk.repository.RoleRepository;
import com.accessrisk.util.AuditDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return roleRepository.findAll()
                .stream()
                .map(RoleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        return RoleResponse.from(findOrThrow(id));
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "A role named '" + request.getName() + "' already exists",
                    ErrorCode.DUPLICATE_ROLE_NAME
            );
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Role saved = roleRepository.save(role);

        auditLogService.log(
                AuditAction.ROLE_CREATED,
                "Role",
                saved.getId(),
                AuditDetails.of(
                        "name", saved.getName(),
                        "description", saved.getDescription()
                )
        );

        log.info("Role created: id={} name={}", saved.getId(), saved.getName());
        return RoleResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Package-visible — used by AssignmentService
    // -------------------------------------------------------------------------

    Role findOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
    }
}
