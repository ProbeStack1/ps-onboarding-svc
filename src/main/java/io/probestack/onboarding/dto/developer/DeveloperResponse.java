package io.probestack.onboarding.dto.developer;

import io.probestack.onboarding.model.DeveloperAccountStatus;
import io.probestack.onboarding.model.DeveloperQuota;
import io.probestack.onboarding.model.DeveloperScopeGrant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperResponse {
    private String id;
    private String organizationId;
    private String employeeId;
    private String email;
    private String firstName;
    private String lastName;
    private String username;
    private String phone;
    private String department;
    private String jobTitle;
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
    private String role;
    private List<String> groups;
    private Map<String, Object> permissions;
    private boolean apiConsumer;
    private boolean apiProvider;
    private boolean aiEngineer;
    private boolean gatewayAdmin;
    private List<DeveloperScopeGrant> scopeGrants;
    private List<String> moduleAccess;
    private String ide;
    private String gitProvider;
    private String defaultRepository;
    private boolean cliAccess;
    private boolean sandboxAccess;
    private boolean productionAccess;
    private List<DeveloperQuota> quotas;
    private DeveloperAccountStatus accountStatus;
    private Instant lastLogin;
    private Instant lastApiCall;
    private Instant lastDeployment;
    private int failedLoginCount;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
