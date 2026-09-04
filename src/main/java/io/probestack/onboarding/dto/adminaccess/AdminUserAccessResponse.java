package io.probestack.onboarding.dto.adminaccess;

import io.probestack.onboarding.dto.access.EffectiveAccessResponse;
import io.probestack.onboarding.dto.member.MemberRoleAssignmentResponse;
import io.probestack.onboarding.dto.member.OrganizationMemberResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserAccessResponse {
    private OrganizationMemberResponse member;
    private List<MemberRoleAssignmentResponse> assignments;
    private EffectiveAccessResponse effectiveAccess;
}
