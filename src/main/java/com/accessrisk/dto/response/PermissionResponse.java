package com.accessrisk.dto.response;

import com.accessrisk.entity.Permission;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PermissionResponse {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public static PermissionResponse from(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .build();
    }
}
