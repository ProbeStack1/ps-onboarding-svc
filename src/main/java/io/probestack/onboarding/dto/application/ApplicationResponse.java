package io.probestack.onboarding.dto.application;

import io.probestack.onboarding.model.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private String id;
    private String organizationId;
    private String businessUnitId;
    private String businessUnitName;
    private String projectId;
    private String projectName;
    private String name;
    private String applicationId;
    private String ownerName;
    private String ownerEmail;
    private String applicationSme;
    private String smeEmail;
    private String testerName;
    private String testerEmail;
    private String serviceNowGroupName;
    private String serviceNowEmail;
    private ApplicationStatus status;
    private long consumerCount;
    private List<String> consumerIds;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
