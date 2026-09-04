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
@Document(collection = "onboarding_principal_role_assignments")
@CompoundIndexes({
        @CompoundIndex(name = "principal_role_subject_idx",
                def = "{'organizationId': 1, 'principalId': 1, 'principalEmail': 1, 'active': 1}"),
        @CompoundIndex(name = "principal_role_scope_idx",
                def = "{'organizationId': 1, 'scopeType': 1, 'scopeId': 1, 'roleKind': 1, 'roleCode': 1, 'active': 1}")
})
public class PrincipalRoleAssignment {
    @Id
    private String id;
    private String organizationId;
    private String principalId;
    private String principalEmail;
    private String principalName;
    private RoleKind roleKind;
    private String roleCode;
    private String scopeType;
    private String scopeId;
    @Builder.Default
    private AssignmentSourceType sourceType = AssignmentSourceType.PRINCIPAL_ASSIGNMENT;
    private String sourceId;
    private String inheritedFrom;
    @Builder.Default
    private boolean active = true;
    private Instant validFrom;
    private Instant validTo;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private String revokedBy;
    private String revokedByEmail;
    private Instant revokedAt;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
