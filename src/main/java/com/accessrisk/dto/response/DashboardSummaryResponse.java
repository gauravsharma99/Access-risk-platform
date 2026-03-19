package com.accessrisk.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardSummaryResponse {

    private long totalUsers;
    private long totalRoles;
    private long totalPermissions;
    private long activeRiskRules;

    // Violation breakdown
    private long openViolations;
    private long criticalViolations;
    private long highViolations;
    private long mediumViolations;
    private long lowViolations;
}
