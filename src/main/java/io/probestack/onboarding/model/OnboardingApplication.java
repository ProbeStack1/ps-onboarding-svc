package io.probestack.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "onboarding_applications")
@CompoundIndexes({
        @CompoundIndex(name = "app_org_application_id_uidx", def = "{'organizationId': 1, 'applicationId': 1}", unique = true),
        @CompoundIndex(name = "app_org_project_status_idx", def = "{'organizationId': 1, 'projectId': 1, 'status': 1, 'updatedAt': -1}")
})
public class OnboardingApplication {
    @Id
    private String id;
    private String organizationId;
    private String businessUnitId;
    private String projectId;
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
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private String deletedBy;
    private String deletedByEmail;
    private Instant deletedAt;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
