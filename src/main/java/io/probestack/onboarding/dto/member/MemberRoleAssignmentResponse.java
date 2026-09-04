package io.probestack.onboarding.dto.member;

import io.probestack.onboarding.model.AssignmentSourceType;
import io.probestack.onboarding.model.RoleKind;
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
public class MemberRoleAssignmentResponse {
    private String id;
    private RoleKind roleKind;
    private String roleCode;
    private String scopeType;
    private String scopeId;
    private String scopeName;
    private AssignmentSourceType sourceType;
    private String sourceId;
    private String inheritedFrom;
    private boolean active;
    private boolean effective;
    private List<String> permissions;
    private Instant validFrom;
    private Instant validTo;
}
