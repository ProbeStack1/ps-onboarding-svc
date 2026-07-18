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
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "onboarding_projects")
@CompoundIndexes({
        @CompoundIndex(name = "project_org_bu_code_uidx", def = "{'organizationId': 1, 'businessUnitId': 1, 'code': 1}", unique = true),
        @CompoundIndex(name = "project_org_bu_status_idx", def = "{'organizationId': 1, 'businessUnitId': 1, 'status': 1, 'updatedAt': -1}")
})
public class OnboardingProject {
    @Id
    private String id;
    private String organizationId;
    private String businessUnitId;
    private String name;
    private String code;
    private String ownerName;
    private String ownerEmail;
    private String projectDlEmail;
    private LocalDate expectedGoLiveDate;
    private String deliveryModel;
    @Builder.Default
    private ProjectStatus status = ProjectStatus.READY;
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
