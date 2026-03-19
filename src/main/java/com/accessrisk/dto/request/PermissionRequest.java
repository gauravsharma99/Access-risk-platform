package com.accessrisk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PermissionRequest {

    /**
     * Permission names follow SCREAMING_SNAKE_CASE convention (e.g. CREATE_VENDOR, APPROVE_PAYMENT)
     * to match the IAG/SAP naming standard and make them instantly recognisable in rule definitions.
     */
    @NotBlank(message = "Permission name is required")
    @Pattern(
        regexp = "^[A-Z][A-Z0-9_]*$",
        message = "Permission name must be UPPER_SNAKE_CASE (e.g. CREATE_VENDOR)"
    )
    @Size(max = 255, message = "Permission name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
