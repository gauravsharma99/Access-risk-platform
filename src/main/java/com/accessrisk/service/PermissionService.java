package com.accessrisk.service;

import com.accessrisk.dto.request.PermissionRequest;
import com.accessrisk.dto.response.PermissionResponse;
import com.accessrisk.entity.Permission;
import com.accessrisk.enums.AuditAction;
import com.accessrisk.enums.ErrorCode;
import com.accessrisk.exception.DuplicateResourceException;
import com.accessrisk.exception.ResourceNotFoundException;
import com.accessrisk.repository.PermissionRepository;
import com.accessrisk.util.AuditDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAll() {
        return permissionRepository.findAll()
                .stream()
                .map(PermissionResponse::from)
                .toList();
    }

    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Permission '" + request.getName() + "' already exists",
                    ErrorCode.DUPLICATE_PERMISSION_NAME
            );
        }

        Permission permission = Permission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Permission saved = permissionRepository.save(permission);

        auditLogService.log(
                AuditAction.PERMISSION_CREATED,
                "Permission",
                saved.getId(),
                AuditDetails.of(
                        "name", saved.getName(),
                        "description", saved.getDescription()
                )
        );

        log.info("Permission created: id={} name={}", saved.getId(), saved.getName());
        return PermissionResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Package-visible — used by AssignmentService and RiskRuleService
    // -------------------------------------------------------------------------

    Permission findOrThrow(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
    }
}
