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
@Document(collection = "onboarding_access_assignments")
@CompoundIndexes({
        @CompoundIndex(name = "access_assignment_email_idx", def = "{'organizationId': 1, 'principalEmail': 1, 'active': 1}"),
        @CompoundIndex(name = "access_assignment_scope_idx", def = "{'organizationId': 1, 'scopeType': 1, 'scopeId': 1, 'role': 1}")
})
public class AccessAssignment {
    @Id
    private String id;
    private String organizationId;
    private String principalEmail;
    private String principalName;
    private AccessRole role;
    private AccessScopeType scopeType;
    private String scopeId;
    @Builder.Default
    private boolean active = true;
    private String createdBy;
    private String createdByEmail;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
