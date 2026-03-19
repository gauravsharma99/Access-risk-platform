package com.accessrisk.dto.response;

import com.accessrisk.entity.RiskRule;
import com.accessrisk.enums.Severity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RiskRuleResponse {

    private Long id;
    private String name;
    private String description;
    private Long permissionAId;
    private String permissionAName;
    private Long permissionBId;
    private String permissionBName;
    private Severity severity;
    private boolean active;
    private LocalDateTime createdAt;

    /**
     * Enriched factory method — requires permission names to be resolved by the service
     * before constructing the response. This keeps the DTO honest: it never goes to the DB.
     */
    public static RiskRuleResponse from(RiskRule rule, String permissionAName, String permissionBName) {
        return RiskRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .permissionAId(rule.getPermissionAId())
                .permissionAName(permissionAName)
                .permissionBId(rule.getPermissionBId())
                .permissionBName(permissionBName)
                .severity(rule.getSeverity())
                .active(rule.isActive())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
