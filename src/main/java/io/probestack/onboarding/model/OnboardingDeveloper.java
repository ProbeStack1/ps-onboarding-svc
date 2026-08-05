package io.probestack.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "onboarding_developers")
@CompoundIndexes({
        @CompoundIndex(name = "developer_org_email_uidx", def = "{'organizationId': 1, 'email': 1}", unique = true),
        @CompoundIndex(name = "developer_org_username_uidx", def = "{'organizationId': 1, 'username': 1}", unique = true),
        @CompoundIndex(name = "developer_org_status_idx", def = "{'organizationId': 1, 'accountStatus': 1, 'updatedAt': -1}"),
        @CompoundIndex(name = "developer_org_scope_idx", def = "{'organizationId': 1, 'businessUnitId': 1, 'projectId': 1, 'applicationId': 1}")
})
public class OnboardingDeveloper {
    @Id
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
    @Builder.Default
    private List<String> groups = new ArrayList<>();
    @Builder.Default
    private Map<String, Object> permissions = new HashMap<>();
    private boolean apiConsumer;
    private boolean apiProvider;
    private boolean aiEngineer;
    private boolean gatewayAdmin;
    @Builder.Default
    private List<DeveloperScopeGrant> scopeGrants = new ArrayList<>();
    @Builder.Default
    private List<String> moduleAccess = new ArrayList<>();
    private String ide;
    private String gitProvider;
    private String defaultRepository;
    private boolean cliAccess;
    private boolean sandboxAccess;
    private boolean productionAccess;
    @Builder.Default
    private List<DeveloperQuota> quotas = new ArrayList<>();
    @Builder.Default
    private DeveloperAccountStatus accountStatus = DeveloperAccountStatus.PENDING_ACTIVATION;
    private Instant lastLogin;
    private Instant lastApiCall;
    private Instant lastDeployment;
    private int failedLoginCount;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private String deletedBy;
    private String deletedByEmail;
    private Instant deletedAt;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
