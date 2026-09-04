package io.probestack.onboarding.dto.member;

import io.probestack.onboarding.model.RoleKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberRoleSummary {
    private RoleKind roleKind;
    private String roleCode;
    private String scopeType;
    private long count;
}
