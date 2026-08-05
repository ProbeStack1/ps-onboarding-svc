package io.probestack.onboarding.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ProjectEnvironment {
    @NotBlank(message = "Environment type is required")
    @Pattern(regexp = "^(Dev|QA|UAT|Performance|Stage|Production)$",
            message = "Environment type is not supported")
    private String environmentType;
    private String endpointUrl;
    @JsonProperty("isEnabled")
    private boolean enabled;
}
