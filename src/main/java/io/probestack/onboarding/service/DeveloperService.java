package io.probestack.onboarding.service;

import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.dto.developer.DeveloperCreateRequest;
import io.probestack.onboarding.dto.developer.DeveloperResponse;
import io.probestack.onboarding.dto.developer.DeveloperUpdateRequest;
import io.probestack.onboarding.exception.DuplicateResourceException;
import io.probestack.onboarding.exception.OrganizationMismatchException;
import io.probestack.onboarding.exception.ResourceNotFoundException;
import io.probestack.onboarding.model.*;
import io.probestack.onboarding.repository.AccessTeamRepository;
import io.probestack.onboarding.repository.DeveloperRepository;
import io.probestack.onboarding.util.ActorResolver;
import io.probestack.onboarding.util.FieldChangeDetector;
import io.probestack.onboarding.util.SlugNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class DeveloperService {
    private static final Set<String> SUPPORTED_MODULES = Set.of(
            "API_DESIGN", "API_LIFECYCLE", "API_TESTING", "API_SECURITY", "API_INVENTORY", "API_GATEWAY",
            "AGENTIC_AI_LIFECYCLE", "AI_MCP_GATEWAY", "AI_INVENTORY", "MCP_LIFECYCLE", "MCP_INVENTORY"
    );

    private final DeveloperRepository developerRepository;
    private final BusinessUnitService businessUnitService;
    private final ProjectService projectService;
    private final ApplicationService applicationService;
    private final AccessTeamRepository accessTeamRepository;
    private final AuditService auditService;
    private final PagingService pagingService;
    private final AccessControlService accessControlService;

    public DeveloperService(DeveloperRepository developerRepository,
                            BusinessUnitService businessUnitService,
                            ProjectService projectService,
                            ApplicationService applicationService,
                            AccessTeamRepository accessTeamRepository,
                            AuditService auditService,
                            PagingService pagingService,
                            AccessControlService accessControlService) {
        this.developerRepository = developerRepository;
        this.businessUnitService = businessUnitService;
        this.projectService = projectService;
        this.applicationService = applicationService;
        this.accessTeamRepository = accessTeamRepository;
        this.auditService = auditService;
        this.pagingService = pagingService;
        this.accessControlService = accessControlService;
    }

    public DeveloperResponse create(String organizationId, DeveloperCreateRequest request, ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        String email = normalizeEmail(request.getEmail());
        String username = normalizeUsername(request.getUsername());
        requireUniqueIdentity(organizationId, null, email, username, SlugNormalizer.trimToNull(request.getEmployeeId()));
        String businessUnitId = request.getBusinessUnitId().trim();
        String projectId = SlugNormalizer.trimToNull(request.getProjectId());
        String applicationId = SlugNormalizer.trimToNull(request.getApplicationId());
        validatePrimaryScope(organizationId, businessUnitId, projectId, applicationId);

        OnboardingDeveloper developer = OnboardingDeveloper.builder()
                .organizationId(organizationId)
                .employeeId(SlugNormalizer.trimToNull(request.getEmployeeId()))
                .email(email)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .username(username)
                .phone(SlugNormalizer.trimToNull(request.getPhone()))
                .department(SlugNormalizer.trimToNull(request.getDepartment()))
                .jobTitle(SlugNormalizer.trimToNull(request.getJobTitle()))
                .businessUnitId(businessUnitId)
                .projectId(projectId)
                .applicationId(applicationId)
                .team(SlugNormalizer.trimToNull(request.getTeam()))
                .ssoEnabled(request.isSsoEnabled())
                .scimEnabled(request.isScimEnabled())
                .mfaEnabled(request.isMfaEnabled())
                .identityProvider(SlugNormalizer.trimToNull(request.getIdentityProvider()))
                .apiTokenAccess(request.isApiTokenAccess())
                .sshKeyFingerprint(SlugNormalizer.trimToNull(request.getSshKeyFingerprint()))
                .role(request.getRole().trim())
                .groups(normalizeStrings(request.getGroups()))
                .permissions(copyPermissions(request.getPermissions()))
                .apiConsumer(request.isApiConsumer())
                .apiProvider(request.isApiProvider())
                .aiEngineer(request.isAiEngineer())
                .gatewayAdmin(request.isGatewayAdmin())
                .scopeGrants(normalizeScopeGrants(organizationId, request.getScopeGrants()))
                .moduleAccess(normalizeModules(request.getModuleAccess()))
                .ide(SlugNormalizer.trimToNull(request.getIde()))
                .gitProvider(SlugNormalizer.trimToNull(request.getGitProvider()))
                .defaultRepository(SlugNormalizer.trimToNull(request.getDefaultRepository()))
                .cliAccess(request.isCliAccess())
                .sandboxAccess(request.isSandboxAccess())
                .productionAccess(request.isProductionAccess())
                .quotas(normalizeQuotas(request.getQuotas(), List.of()))
                .accountStatus(request.getAccountStatus() == null ? DeveloperAccountStatus.PENDING_ACTIVATION : request.getAccountStatus())
                .failedLoginCount(0)
                .createdBy(actor.name())
                .createdByEmail(actor.email())
                .updatedBy(actor.name())
                .updatedByEmail(actor.email())
                .build();
        OnboardingDeveloper saved = developerRepository.save(developer);
        auditService.record(organizationId, ResourceType.DEVELOPER, saved.getId(), AuditAction.CREATE, actor, List.of(), null, saved);
        return toResponse(saved);
    }

    public PagedResult<DeveloperResponse> list(String organizationId, String search, DeveloperAccountStatus status,
                                                int page, int size, ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        List<OnboardingDeveloper> source = status == null
                ? developerRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId)
                : developerRepository.findByOrganizationIdAndAccountStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, status);
        List<DeveloperResponse> filtered = source.stream()
                .filter(item -> matches(search, item.getFirstName(), item.getLastName(), item.getEmail(), item.getUsername(), item.getEmployeeId(), item.getDepartment()))
                .map(this::toResponse)
                .toList();
        return pagingService.page(filtered, page, size);
    }

    public DeveloperResponse get(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        return toResponse(find(organizationId, id));
    }

    public DeveloperResponse update(String organizationId, String id, DeveloperUpdateRequest request, ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        OnboardingDeveloper developer = find(organizationId, id);
        Map<String, Object> before = developerFields(developer);

        String nextEmail = StringUtils.hasText(request.getEmail()) ? normalizeEmail(request.getEmail()) : developer.getEmail();
        String nextUsername = StringUtils.hasText(request.getUsername()) ? normalizeUsername(request.getUsername()) : developer.getUsername();
        String nextEmployeeId = request.getEmployeeId() == null ? developer.getEmployeeId() : SlugNormalizer.trimToNull(request.getEmployeeId());
        requireUniqueIdentity(organizationId, developer, nextEmail, nextUsername, nextEmployeeId);

        String nextBusinessUnitId = request.getBusinessUnitId() == null ? developer.getBusinessUnitId() : SlugNormalizer.trimToNull(request.getBusinessUnitId());
        if (nextBusinessUnitId == null) throw new IllegalArgumentException("Business unit id is required");
        String nextProjectId = request.getProjectId() == null ? developer.getProjectId() : SlugNormalizer.trimToNull(request.getProjectId());
        String nextApplicationId = request.getApplicationId() == null ? developer.getApplicationId() : SlugNormalizer.trimToNull(request.getApplicationId());
        validatePrimaryScope(organizationId, nextBusinessUnitId, nextProjectId, nextApplicationId);

        developer.setEmail(nextEmail);
        developer.setUsername(nextUsername);
        developer.setEmployeeId(nextEmployeeId);
        developer.setBusinessUnitId(nextBusinessUnitId);
        developer.setProjectId(nextProjectId);
        developer.setApplicationId(nextApplicationId);
        if (StringUtils.hasText(request.getFirstName())) developer.setFirstName(request.getFirstName().trim());
        if (StringUtils.hasText(request.getLastName())) developer.setLastName(request.getLastName().trim());
        if (request.getPhone() != null) developer.setPhone(SlugNormalizer.trimToNull(request.getPhone()));
        if (request.getDepartment() != null) developer.setDepartment(SlugNormalizer.trimToNull(request.getDepartment()));
        if (request.getJobTitle() != null) developer.setJobTitle(SlugNormalizer.trimToNull(request.getJobTitle()));
        if (request.getTeam() != null) developer.setTeam(SlugNormalizer.trimToNull(request.getTeam()));
        if (request.getSsoEnabled() != null) developer.setSsoEnabled(request.getSsoEnabled());
        if (request.getScimEnabled() != null) developer.setScimEnabled(request.getScimEnabled());
        if (request.getMfaEnabled() != null) developer.setMfaEnabled(request.getMfaEnabled());
        if (request.getIdentityProvider() != null) developer.setIdentityProvider(SlugNormalizer.trimToNull(request.getIdentityProvider()));
        if (request.getApiTokenAccess() != null) developer.setApiTokenAccess(request.getApiTokenAccess());
        if (request.getSshKeyFingerprint() != null) developer.setSshKeyFingerprint(SlugNormalizer.trimToNull(request.getSshKeyFingerprint()));
        if (StringUtils.hasText(request.getRole())) developer.setRole(request.getRole().trim());
        if (request.getGroups() != null) developer.setGroups(normalizeStrings(request.getGroups()));
        if (request.getPermissions() != null) developer.setPermissions(copyPermissions(request.getPermissions()));
        if (request.getApiConsumer() != null) developer.setApiConsumer(request.getApiConsumer());
        if (request.getApiProvider() != null) developer.setApiProvider(request.getApiProvider());
        if (request.getAiEngineer() != null) developer.setAiEngineer(request.getAiEngineer());
        if (request.getGatewayAdmin() != null) developer.setGatewayAdmin(request.getGatewayAdmin());
        if (request.getScopeGrants() != null) developer.setScopeGrants(normalizeScopeGrants(organizationId, request.getScopeGrants()));
        if (request.getModuleAccess() != null) developer.setModuleAccess(normalizeModules(request.getModuleAccess()));
        if (request.getIde() != null) developer.setIde(SlugNormalizer.trimToNull(request.getIde()));
        if (request.getGitProvider() != null) developer.setGitProvider(SlugNormalizer.trimToNull(request.getGitProvider()));
        if (request.getDefaultRepository() != null) developer.setDefaultRepository(SlugNormalizer.trimToNull(request.getDefaultRepository()));
        if (request.getCliAccess() != null) developer.setCliAccess(request.getCliAccess());
        if (request.getSandboxAccess() != null) developer.setSandboxAccess(request.getSandboxAccess());
        if (request.getProductionAccess() != null) developer.setProductionAccess(request.getProductionAccess());
        if (request.getQuotas() != null) developer.setQuotas(normalizeQuotas(request.getQuotas(), developer.getQuotas()));
        if (request.getAccountStatus() != null) developer.setAccountStatus(request.getAccountStatus());
        developer.setUpdatedBy(actor.name());
        developer.setUpdatedByEmail(actor.email());

        OnboardingDeveloper saved = developerRepository.save(developer);
        var changes = FieldChangeDetector.diff(before, developerFields(saved));
        AuditAction action = !Objects.equals(before.get("accountStatus"), saved.getAccountStatus()) ? AuditAction.STATUS_CHANGE : AuditAction.UPDATE;
        auditService.record(organizationId, ResourceType.DEVELOPER, id, action, actor, changes, before, saved);
        return toResponse(saved);
    }

    public void delete(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        OnboardingDeveloper developer = find(organizationId, id);
        Map<String, Object> before = developerFields(developer);
        developer.setAccountStatus(DeveloperAccountStatus.INACTIVE);
        developer.setDeletedAt(Instant.now());
        developer.setDeletedBy(actor.name());
        developer.setDeletedByEmail(actor.email());
        developer.setUpdatedBy(actor.name());
        developer.setUpdatedByEmail(actor.email());
        OnboardingDeveloper saved = developerRepository.save(developer);
        auditService.record(organizationId, ResourceType.DEVELOPER, id, AuditAction.DELETE, actor,
                FieldChangeDetector.diff(before, developerFields(saved)), before, saved);
    }

    public List<AuditLogResponse> history(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        find(organizationId, id);
        return auditService.history(organizationId, ResourceType.DEVELOPER, id);
    }

    public OnboardingDeveloper find(String organizationId, String id) {
        return developerRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer not found: " + id));
    }

    public DeveloperResponse toResponse(OnboardingDeveloper developer) {
        return DeveloperResponse.builder()
                .id(developer.getId())
                .organizationId(developer.getOrganizationId())
                .employeeId(developer.getEmployeeId())
                .email(developer.getEmail())
                .firstName(developer.getFirstName())
                .lastName(developer.getLastName())
                .username(developer.getUsername())
                .phone(developer.getPhone())
                .department(developer.getDepartment())
                .jobTitle(developer.getJobTitle())
                .businessUnitId(developer.getBusinessUnitId())
                .projectId(developer.getProjectId())
                .applicationId(developer.getApplicationId())
                .team(developer.getTeam())
                .ssoEnabled(developer.isSsoEnabled())
                .scimEnabled(developer.isScimEnabled())
                .mfaEnabled(developer.isMfaEnabled())
                .identityProvider(developer.getIdentityProvider())
                .apiTokenAccess(developer.isApiTokenAccess())
                .sshKeyFingerprint(developer.getSshKeyFingerprint())
                .role(developer.getRole())
                .groups(safeList(developer.getGroups()))
                .permissions(developer.getPermissions() == null ? Map.of() : developer.getPermissions())
                .apiConsumer(developer.isApiConsumer())
                .apiProvider(developer.isApiProvider())
                .aiEngineer(developer.isAiEngineer())
                .gatewayAdmin(developer.isGatewayAdmin())
                .scopeGrants(safeList(developer.getScopeGrants()))
                .moduleAccess(safeList(developer.getModuleAccess()))
                .ide(developer.getIde())
                .gitProvider(developer.getGitProvider())
                .defaultRepository(developer.getDefaultRepository())
                .cliAccess(developer.isCliAccess())
                .sandboxAccess(developer.isSandboxAccess())
                .productionAccess(developer.isProductionAccess())
                .quotas(safeList(developer.getQuotas()))
                .accountStatus(developer.getAccountStatus())
                .lastLogin(developer.getLastLogin())
                .lastApiCall(developer.getLastApiCall())
                .lastDeployment(developer.getLastDeployment())
                .failedLoginCount(developer.getFailedLoginCount())
                .createdBy(developer.getCreatedBy())
                .createdByEmail(developer.getCreatedByEmail())
                .updatedBy(developer.getUpdatedBy())
                .updatedByEmail(developer.getUpdatedByEmail())
                .createdAt(developer.getCreatedAt())
                .updatedAt(developer.getUpdatedAt())
                .build();
    }

    private void requireUniqueIdentity(String organizationId, OnboardingDeveloper current, String email, String username, String employeeId) {
        if ((current == null || !email.equals(current.getEmail())) && developerRepository.existsByOrganizationIdAndEmail(organizationId, email)) {
            throw new DuplicateResourceException("Developer email already exists for this organization: " + email);
        }
        if ((current == null || !username.equals(current.getUsername())) && developerRepository.existsByOrganizationIdAndUsername(organizationId, username)) {
            throw new DuplicateResourceException("Developer username already exists for this organization: " + username);
        }
        if (employeeId != null && (current == null || !employeeId.equals(current.getEmployeeId()))
                && developerRepository.existsByOrganizationIdAndEmployeeId(organizationId, employeeId)) {
            throw new DuplicateResourceException("Developer employee id already exists for this organization: " + employeeId);
        }
    }

    private void validatePrimaryScope(String organizationId, String businessUnitId, String projectId, String applicationId) {
        businessUnitService.find(organizationId, businessUnitId);
        OnboardingProject project = projectId == null ? null : projectService.find(organizationId, projectId);
        if (project != null && !businessUnitId.equals(project.getBusinessUnitId())) {
            throw new OrganizationMismatchException("Project does not belong to the selected business unit");
        }
        if (applicationId != null) {
            if (project == null) throw new IllegalArgumentException("A project is required when an application scope is selected");
            OnboardingApplication application = applicationService.find(organizationId, applicationId);
            if (!businessUnitId.equals(application.getBusinessUnitId()) || !projectId.equals(application.getProjectId())) {
                throw new OrganizationMismatchException("Application does not belong to the selected project and business unit");
            }
        }
    }

    private List<DeveloperScopeGrant> normalizeScopeGrants(String organizationId, List<DeveloperScopeGrant> grants) {
        if (grants == null) return new ArrayList<>();
        Set<String> keys = new HashSet<>();
        List<DeveloperScopeGrant> normalized = new ArrayList<>();
        for (DeveloperScopeGrant grant : grants) {
            String levelCode = requiredText(grant.getLevelCode(), "RBAC level code is required").toUpperCase(Locale.ROOT);
            String scopeType = requiredText(grant.getScopeType(), "Scope type is required").replace(' ', '_').toUpperCase(Locale.ROOT);
            String scopeId = requiredText(grant.getScopeId(), "Scope id is required");
            validateGrantScope(organizationId, scopeType, scopeId);
            String key = levelCode + ":" + scopeType + ":" + scopeId;
            if (!keys.add(key)) throw new IllegalArgumentException("Duplicate scoped role grant: " + key);
            normalized.add(DeveloperScopeGrant.builder().levelCode(levelCode).scopeType(scopeType).scopeId(scopeId).build());
        }
        return normalized;
    }

    private void validateGrantScope(String organizationId, String scopeType, String scopeId) {
        switch (scopeType) {
            case "ORGANIZATION" -> {
                if (!organizationId.equals(scopeId)) throw new OrganizationMismatchException("Organization grant scope does not match the active organization");
            }
            case "BUSINESS_UNIT" -> businessUnitService.find(organizationId, scopeId);
            case "PROJECT" -> projectService.find(organizationId, scopeId);
            case "APPLICATION" -> applicationService.find(organizationId, scopeId);
            case "TEAM" -> accessTeamRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(scopeId, organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Access team not found: " + scopeId));
            default -> throw new IllegalArgumentException("Scope type is not supported: " + scopeType);
        }
    }

    private List<String> normalizeModules(List<String> modules) {
        if (modules == null) return new ArrayList<>();
        List<String> normalized = modules.stream()
                .map(SlugNormalizer::trimToNull)
                .filter(Objects::nonNull)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        List<String> unsupported = normalized.stream().filter(value -> !SUPPORTED_MODULES.contains(value)).toList();
        if (!unsupported.isEmpty()) throw new IllegalArgumentException("Unsupported platform modules: " + String.join(", ", unsupported));
        return normalized;
    }

    private List<DeveloperQuota> normalizeQuotas(List<DeveloperQuota> requested, List<DeveloperQuota> existing) {
        if (requested == null) return new ArrayList<>();
        Map<String, BigDecimal> usageByType = new HashMap<>();
        safeList(existing).forEach(quota -> usageByType.put(quota.getQuotaType(), quota.getQuotaUsed() == null ? BigDecimal.ZERO : quota.getQuotaUsed()));
        Set<String> types = new HashSet<>();
        List<DeveloperQuota> normalized = new ArrayList<>();
        for (DeveloperQuota quota : requested) {
            String type = requiredText(quota.getQuotaType(), "Quota type is required");
            if (!types.add(type)) throw new IllegalArgumentException("Each quota type may be configured only once");
            normalized.add(DeveloperQuota.builder()
                    .quotaType(type)
                    .quotaLimit(quota.getQuotaLimit())
                    .quotaUsed(usageByType.getOrDefault(type, BigDecimal.ZERO))
                    .build());
        }
        return normalized;
    }

    private List<String> normalizeStrings(List<String> values) {
        if (values == null) return new ArrayList<>();
        return values.stream().map(SlugNormalizer::trimToNull).filter(Objects::nonNull).distinct().toList();
    }

    private Map<String, Object> copyPermissions(Map<String, Object> permissions) {
        return permissions == null ? new HashMap<>() : new HashMap<>(permissions);
    }

    private String normalizeEmail(String email) {
        return requiredText(email, "Developer email is required").toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return requiredText(username, "Username is required").toLowerCase(Locale.ROOT);
    }

    private String requiredText(String value, String message) {
        String normalized = SlugNormalizer.trimToNull(value);
        if (normalized == null) throw new IllegalArgumentException(message);
        return normalized;
    }

    private boolean matches(String search, String... values) {
        if (!StringUtils.hasText(search)) return true;
        String needle = search.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values).anyMatch(value -> value != null && value.toLowerCase(Locale.ROOT).contains(needle));
    }

    private Map<String, Object> developerFields(OnboardingDeveloper developer) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("employeeId", value(developer.getEmployeeId()));
        fields.put("email", value(developer.getEmail()));
        fields.put("firstName", value(developer.getFirstName()));
        fields.put("lastName", value(developer.getLastName()));
        fields.put("username", value(developer.getUsername()));
        fields.put("phone", value(developer.getPhone()));
        fields.put("department", value(developer.getDepartment()));
        fields.put("jobTitle", value(developer.getJobTitle()));
        fields.put("businessUnitId", value(developer.getBusinessUnitId()));
        fields.put("projectId", value(developer.getProjectId()));
        fields.put("applicationId", value(developer.getApplicationId()));
        fields.put("team", value(developer.getTeam()));
        fields.put("ssoEnabled", developer.isSsoEnabled());
        fields.put("scimEnabled", developer.isScimEnabled());
        fields.put("mfaEnabled", developer.isMfaEnabled());
        fields.put("identityProvider", value(developer.getIdentityProvider()));
        fields.put("apiTokenAccess", developer.isApiTokenAccess());
        fields.put("sshKeyFingerprint", value(developer.getSshKeyFingerprint()));
        fields.put("role", value(developer.getRole()));
        fields.put("groups", safeList(developer.getGroups()));
        fields.put("permissions", developer.getPermissions() == null ? Map.of() : developer.getPermissions());
        fields.put("apiConsumer", developer.isApiConsumer());
        fields.put("apiProvider", developer.isApiProvider());
        fields.put("aiEngineer", developer.isAiEngineer());
        fields.put("gatewayAdmin", developer.isGatewayAdmin());
        fields.put("scopeGrants", safeList(developer.getScopeGrants()));
        fields.put("moduleAccess", safeList(developer.getModuleAccess()));
        fields.put("ide", value(developer.getIde()));
        fields.put("gitProvider", value(developer.getGitProvider()));
        fields.put("defaultRepository", value(developer.getDefaultRepository()));
        fields.put("cliAccess", developer.isCliAccess());
        fields.put("sandboxAccess", developer.isSandboxAccess());
        fields.put("productionAccess", developer.isProductionAccess());
        fields.put("quotas", safeList(developer.getQuotas()));
        fields.put("accountStatus", developer.getAccountStatus());
        return fields;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }
}
