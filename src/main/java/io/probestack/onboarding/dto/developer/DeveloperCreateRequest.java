package io.probestack.onboarding.dto.developer;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.DeveloperAccountStatus;
import io.probestack.onboarding.model.DeveloperQuota;
import io.probestack.onboarding.model.DeveloperScopeGrant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DeveloperCreateRequest {
    private String employeeId;
    @NotBlank(message = "Developer email is required")
    @Email(message = "Developer email must be valid")
    private String email;
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username contains unsupported characters")
    private String username;
    private String phone;
    private String department;
    private String jobTitle;
    @NotBlank(message = "Business unit id is required")
    private String businessUnitId;
    private String projectId;
    private String applicationId;
    private String team;
    private boolean ssoEnabled;
    private boolean scimEnabled;
    private boolean mfaEnabled;
    private String identityProvider;
    private boolean apiTokenAccess;
    private String sshKeyFingerprint;
    @NotBlank(message = "Primary role is required")
    private String role;
    private List<String> groups = new ArrayList<>();
    private Map<String, Object> permissions = new HashMap<>();
    private boolean apiConsumer;
    private boolean apiProvider;
    private boolean aiEngineer;
    private boolean gatewayAdmin;
    @Valid
    private List<DeveloperScopeGrant> scopeGrants = new ArrayList<>();
    private List<String> moduleAccess = new ArrayList<>();
    private String ide;
    private String gitProvider;
    private String defaultRepository;
    private boolean cliAccess;
    private boolean sandboxAccess;
    private boolean productionAccess;
    @Valid
    private List<DeveloperQuota> quotas = new ArrayList<>();
    private DeveloperAccountStatus accountStatus = DeveloperAccountStatus.PENDING_ACTIVATION;
    private ActorDTO actor;
}
