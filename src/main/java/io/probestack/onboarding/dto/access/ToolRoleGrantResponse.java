package io.probestack.onboarding.dto.access;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolRoleGrantResponse {
    private String toolKey;
    private String toolName;
    private String role;
}
