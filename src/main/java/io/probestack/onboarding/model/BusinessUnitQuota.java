package io.probestack.onboarding.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessUnitQuota {
    @NotBlank(message = "Quota type is required")
    @Pattern(regexp = "^(api_calls|ai_tokens|storage|compute_hours|agent_runtime|mcp_connections)$",
            message = "Quota type is not supported")
    private String quotaType;

    @NotNull(message = "Quota limit is required")
    @DecimalMin(value = "0.0", message = "Quota limit must be zero or greater")
    private BigDecimal quotaLimit;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "Quota usage must be zero or greater")
    private BigDecimal quotaUsed = BigDecimal.ZERO;
}
