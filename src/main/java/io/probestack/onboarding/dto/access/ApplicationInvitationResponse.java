package io.probestack.onboarding.dto.access;

import io.probestack.onboarding.model.AccessRole;
import io.probestack.onboarding.model.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationInvitationResponse {
    private String id;
    private String applicationId;
    private String applicationName;
    private String invitedEmail;
    private String invitedName;
    private AccessRole role;
    private List<ToolRoleGrantResponse> toolRoleGrants;
    private InvitationStatus status;
    private String message;
    private String createdByEmail;
    private String acceptedByEmail;
    private Instant acceptedAt;
    private String rejectedByEmail;
    private Instant rejectedAt;
    private String revokedByEmail;
    private Instant revokedAt;
    private Instant createdAt;
}
