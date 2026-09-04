package io.probestack.onboarding.dto.member;

import io.probestack.onboarding.model.RoleKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.Instant;

@Data
public class RoleAssignmentCreateRequest {
    @NotBlank
    private String principalId;
    private String principalEmail;
    private String principalName;
    @NotNull
    private RoleKind roleKind;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    private String roleCode;
    @NotBlank
    @Pattern(regexp = "^(ORGANIZATION|BUSINESS_UNIT|PROJECT|APPLICATION|TEAM|TOOL)$")
    private String scopeType;
    @NotBlank
    private String scopeId;
    private Instant validFrom;
    private Instant validTo;
}
