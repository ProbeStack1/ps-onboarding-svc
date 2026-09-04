package io.probestack.onboarding.dto.adminaccess;

import io.probestack.onboarding.dto.member.MemberRoleAssignmentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourcePrincipalAccessResponse {
    private String principalId;
    private String email;
    private String name;
    private String organizationRole;
    private boolean canView;
    private boolean canManage;
    private boolean applicationMember;
    private List<MemberRoleAssignmentResponse> contributingAssignments;
}
