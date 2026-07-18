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
@Document(collection = "onboarding_business_units")
@CompoundIndexes({
        @CompoundIndex(name = "bu_org_code_uidx", def = "{'organizationId': 1, 'code': 1}", unique = true),
        @CompoundIndex(name = "bu_org_status_idx", def = "{'organizationId': 1, 'status': 1, 'updatedAt': -1}")
})
public class BusinessUnit {
    @Id
    private String id;
    private String organizationId;
    private String name;
    private String code;
    private String ownerName;
    private String ownerEmail;
    private String costCenter;
    private String description;
    @Builder.Default
    private BusinessUnitStatus status = BusinessUnitStatus.ACTIVE;
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
