package io.probestack.onboarding.dto.adminaccess;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessBootstrapResponse {
    private AdminUserAccessResponse loginAccess;
    private TokenAccessClaims tokenClaims;
}
