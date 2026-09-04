package io.probestack.onboarding.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberResponse {
    private String principalId;
    private String organizationId;
    private String email;
    private String name;
    private String organizationRole;
    private String accountStatus;
    private boolean active;
    private boolean developerProfileConfigured;
    private String developerProfileId;
    private String developerRole;
    private long assignmentCount;
    private List<MemberRoleSummary> roles;
}
