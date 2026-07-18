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
@Document(collection = "onboarding_consumers")
@CompoundIndexes({
        @CompoundIndex(name = "consumer_org_name_idx", def = "{'organizationId': 1, 'name': 1}"),
        @CompoundIndex(name = "consumer_org_status_idx", def = "{'organizationId': 1, 'status': 1, 'updatedAt': -1}")
})
public class Consumer {
    @Id
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
    @Builder.Default
    private ConsumerStatus status = ConsumerStatus.ACTIVE;
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
