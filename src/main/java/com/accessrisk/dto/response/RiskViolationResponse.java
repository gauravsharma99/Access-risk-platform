package com.accessrisk.dto.response;

import com.accessrisk.entity.RiskViolation;
import com.accessrisk.enums.Severity;
import com.accessrisk.enums.ViolationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RiskViolationResponse {

    private Long id;
    private Long userId;
    private String userName;         // resolved from User entity by service
    private Long ruleId;
    private String ruleName;         // resolved from RiskRule entity by service
    private Severity severity;
    private ViolationStatus status;
    private String explanation;
    private LocalDateTime detectedAt;

    public static RiskViolationResponse from(
            RiskViolation violation,
            String userName,
            String ruleName) {

        return RiskViolationResponse.builder()
                .id(violation.getId())
                .userId(violation.getUserId())
                .userName(userName)
                .ruleId(violation.getRuleId())
                .ruleName(ruleName)
                .severity(violation.getSeverity())
                .status(violation.getStatus())
                .explanation(violation.getExplanation())
                .detectedAt(violation.getDetectedAt())
                .build();
    }
}
