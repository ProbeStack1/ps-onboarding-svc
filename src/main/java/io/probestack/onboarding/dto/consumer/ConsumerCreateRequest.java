package io.probestack.onboarding.dto.consumer;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.ConsumerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConsumerCreateRequest {
    @NotBlank(message = "Consumer name is required")
    private String name;
    private String pocName;
    @Email(message = "Consumer POC email must be valid")
    private String pocEmail;
    private String smeName;
    @Email(message = "Consumer SME email must be valid")
    private String smeEmail;
    private String consumerConfig;
    @Min(value = 0, message = "API TPS cannot be negative")
    private Integer apiTps;
    @Min(value = 0, message = "Quota cannot be negative")
    private Integer quotaRequestsPerDay;
    @Min(value = 0, message = "Rate limit cannot be negative")
    private Integer rateLimitRequestsPerMinute;
    private String apiKeyInformation;
    private ConsumerStatus status = ConsumerStatus.ACTIVE;
    private ActorDTO actor;
}
