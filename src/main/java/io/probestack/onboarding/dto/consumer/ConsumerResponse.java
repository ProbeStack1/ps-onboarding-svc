package io.probestack.onboarding.dto.consumer;

import io.probestack.onboarding.model.ConsumerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerResponse {
    private String id;
    private String organizationId;
    private String name;
    private String pocName;
    private String pocEmail;
    private String smeName;
    private String smeEmail;
    private String consumerConfig;
    private Integer apiTps;
    private Integer quotaRequestsPerDay;
    private Integer rateLimitRequestsPerMinute;
    private String apiKeyInformation;
    private ConsumerStatus status;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
