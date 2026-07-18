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
@Document(collection = "onboarding_application_consumers")
@CompoundIndexes({
        @CompoundIndex(name = "app_consumer_org_app_consumer_uidx", def = "{'organizationId': 1, 'applicationId': 1, 'consumerId': 1}", unique = true),
        @CompoundIndex(name = "app_consumer_org_app_idx", def = "{'organizationId': 1, 'applicationId': 1}")
})
public class ApplicationConsumerLink {
    @Id
    private String id;
    private String organizationId;
    private String applicationId;
    private String consumerId;
    private String createdBy;
    private String createdByEmail;
    @CreatedDate
    private Instant createdAt;
}
