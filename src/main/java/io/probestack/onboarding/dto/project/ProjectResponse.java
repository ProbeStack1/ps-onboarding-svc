package io.probestack.onboarding.dto.project;

import io.probestack.onboarding.model.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private String id;
    private String organizationId;
    private String businessUnitId;
    private String businessUnitName;
    private String name;
    private String code;
    private String ownerName;
    private String ownerEmail;
    private String projectDlEmail;
    private LocalDate expectedGoLiveDate;
    private String deliveryModel;
    private ProjectStatus status;
    private long applicationCount;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
