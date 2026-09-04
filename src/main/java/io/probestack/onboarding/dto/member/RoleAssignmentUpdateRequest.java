package io.probestack.onboarding.dto.member;

import lombok.Data;

import java.time.Instant;

@Data
public class RoleAssignmentUpdateRequest {
    private Boolean active;
    private Instant validFrom;
    private Instant validTo;
}
