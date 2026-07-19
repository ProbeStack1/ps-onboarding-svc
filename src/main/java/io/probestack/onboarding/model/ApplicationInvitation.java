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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "onboarding_application_invitations")
@CompoundIndexes({
        @CompoundIndex(name = "app_invitation_email_idx", def = "{'organizationId': 1, 'invitedEmail': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "app_invitation_app_idx", def = "{'organizationId': 1, 'applicationId': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "app_invitation_accepted_idx", def = "{'organizationId': 1, 'acceptedByEmail': 1, 'status': 1}")
})
public class ApplicationInvitation {
    @Id
    private String id;
    private String organizationId;
    private String applicationId;
    private String invitedEmail;
    private String invitedName;
    private AccessRole role;
    private List<ToolRoleGrant> toolRoleGrants;
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;
    private String message;
    private String createdBy;
    private String createdByEmail;
    private String acceptedByEmail;
    private Instant acceptedAt;
    private String rejectedByEmail;
    private Instant rejectedAt;
    private String revokedByEmail;
    private Instant revokedAt;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
