package io.probestack.onboarding.dto.adminaccess;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class TokenAccessClaims {
    @JsonProperty("organization_id")
    private String organizationId;
    @JsonProperty("sub")
    private String principalId;
    private String email;
    private String role;
    @JsonProperty("is_org_admin")
    private boolean orgAdmin;
    @JsonProperty("view_business_unit_ids")
    private List<String> viewBusinessUnitIds;
    @JsonProperty("manage_business_unit_ids")
    private List<String> manageBusinessUnitIds;
    @JsonProperty("view_project_ids")
    private List<String> viewProjectIds;
    @JsonProperty("manage_project_ids")
    private List<String> manageProjectIds;
    @JsonProperty("view_application_ids")
    private List<String> viewApplicationIds;
    @JsonProperty("manage_application_ids")
    private List<String> manageApplicationIds;
    @JsonProperty("member_application_ids")
    private List<String> memberApplicationIds;
    @JsonProperty("role_assignments")
    private List<CompactRoleClaim> roleAssignments;
    @JsonProperty("access_generated_at")
    private Instant accessGeneratedAt;
}
