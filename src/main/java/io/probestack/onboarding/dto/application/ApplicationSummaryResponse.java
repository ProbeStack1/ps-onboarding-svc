package io.probestack.onboarding.dto.application;

import io.probestack.onboarding.model.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSummaryResponse {
    private String id;
    private String name;
    private String applicationId;
    private String ownerName;
    private ApplicationStatus status;
    private long consumerCount;
}
