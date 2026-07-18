package io.probestack.onboarding.dto.access;

import io.probestack.onboarding.model.AccessRole;
import io.probestack.onboarding.model.AccessScopeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessAssignmentResponse {
    private String id;
    private String principalEmail;
    private String principalName;
    private AccessRole role;
    private AccessScopeType scopeType;
    private String scopeId;
    private boolean active;
    private Instant createdAt;
}
