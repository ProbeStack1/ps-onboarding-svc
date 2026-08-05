package io.probestack.onboarding.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperScopeGrant {
    @NotBlank(message = "RBAC level code is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "RBAC level code contains unsupported characters")
    private String levelCode;

    @NotBlank(message = "Scope type is required")
    @Pattern(regexp = "^(ORGANIZATION|BUSINESS_UNIT|PROJECT|APPLICATION|TEAM)$", message = "Scope type is not supported")
    private String scopeType;

    @NotBlank(message = "Scope id is required")
    private String scopeId;
}
