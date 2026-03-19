package com.accessrisk.dto.request;

import com.accessrisk.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RiskRuleRequest {

    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Permission A ID is required")
    private Long permissionAId;

    @NotNull(message = "Permission B ID is required")
    private Long permissionBId;

    @NotNull(message = "Severity is required")
    private Severity severity;
}
