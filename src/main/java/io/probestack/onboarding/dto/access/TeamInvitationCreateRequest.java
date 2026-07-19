package io.probestack.onboarding.dto.access;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamInvitationCreateRequest {
    @NotBlank(message = "Invited email is required")
    @Email(message = "Invited email must be valid")
    private String invitedEmail;
    private String invitedName;
    @Valid
    private List<ToolRoleGrantRequest> toolRoleGrants;
}
