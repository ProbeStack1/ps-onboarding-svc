package io.probestack.onboarding.dto.access;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolRoleGrantRequest {
    @NotBlank(message = "Tool key is required")
    private String toolKey;

    @NotBlank(message = "Tool name is required")
    private String toolName;

    @NotBlank(message = "Role is required")
    private String role;
}
