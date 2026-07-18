package io.probestack.onboarding.dto.access;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveAccessResponse {
    private String organizationId;
    private String userEmail;
    private boolean orgAdmin;
    private Set<String> viewBusinessUnitIds;
    private Set<String> manageBusinessUnitIds;
    private Set<String> viewProjectIds;
    private Set<String> manageProjectIds;
    private Set<String> viewApplicationIds;
    private Set<String> manageApplicationIds;
    private Set<String> memberApplicationIds;
}
