package io.probestack.onboarding.dto.member;

import io.probestack.onboarding.dto.access.EffectiveAccessResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberAccessResponse {
    private OrganizationMemberResponse member;
    private List<MemberRoleAssignmentResponse> assignments;
    private EffectiveAccessResponse effectiveAccess;
}
