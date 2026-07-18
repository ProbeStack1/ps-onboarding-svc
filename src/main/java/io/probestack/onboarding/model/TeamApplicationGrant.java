package io.probestack.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "onboarding_team_application_grants")
@CompoundIndexes({
        @CompoundIndex(name = "team_app_grant_uidx", def = "{'organizationId': 1, 'teamId': 1, 'applicationId': 1}", unique = true),
        @CompoundIndex(name = "team_app_grant_app_idx", def = "{'organizationId': 1, 'applicationId': 1}")
})
public class TeamApplicationGrant {
    @Id
    private String id;
    private String organizationId;
    private String teamId;
    private String applicationId;
    private String createdBy;
    private String createdByEmail;
    @CreatedDate
    private Instant createdAt;
}
