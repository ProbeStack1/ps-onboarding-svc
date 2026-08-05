package io.probestack.onboarding.dto.developer;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.DeveloperAccountStatus;
import io.probestack.onboarding.model.DeveloperQuota;
import io.probestack.onboarding.model.DeveloperScopeGrant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DeveloperUpdateRequest {
    private String employeeId;
    @Email(message = "Developer email must be valid")
    private String email;
    private String firstName;
    private String lastName;
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username contains unsupported characters")
    private String username;
    private String phone;
    private String department;
    private String jobTitle;
    private String businessUnitId;
    private String projectId;
    private String applicationId;
    private String team;
    private Boolean ssoEnabled;
    private Boolean scimEnabled;
    private Boolean mfaEnabled;
    private String identityProvider;
    private Boolean apiTokenAccess;
    private String sshKeyFingerprint;
    private String role;
    private List<String> groups;
    private Map<String, Object> permissions;
    private Boolean apiConsumer;
    private Boolean apiProvider;
    private Boolean aiEngineer;
    private Boolean gatewayAdmin;
    @Valid
    private List<DeveloperScopeGrant> scopeGrants;
    private List<String> moduleAccess;
    private String ide;
    private String gitProvider;
    private String defaultRepository;
    private Boolean cliAccess;
    private Boolean sandboxAccess;
    private Boolean productionAccess;
    @Valid
    private List<DeveloperQuota> quotas;
    private DeveloperAccountStatus accountStatus;
    private ActorDTO actor;
}
