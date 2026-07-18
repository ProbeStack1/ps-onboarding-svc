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
@Document(collection = "onboarding_access_teams")
@CompoundIndexes({
        @CompoundIndex(name = "access_team_org_idx", def = "{'organizationId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "access_team_scope_idx", def = "{'organizationId': 1, 'businessUnitId': 1, 'projectId': 1, 'applicationId': 1}")
})
public class AccessTeam {
    @Id
    private String id;
    private String organizationId;
    private String name;
    private String description;
    private String businessUnitId;
    private String projectId;
    private String applicationId;
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
