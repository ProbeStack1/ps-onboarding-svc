package io.probestack.onboarding.service;

import io.probestack.onboarding.dto.application.ApplicationSummaryResponse;
import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.dto.project.ProjectCreateRequest;
import io.probestack.onboarding.dto.project.ProjectResponse;
import io.probestack.onboarding.dto.project.ProjectUpdateRequest;
import io.probestack.onboarding.exception.DuplicateResourceException;
import io.probestack.onboarding.exception.InvalidStatusTransitionException;
import io.probestack.onboarding.exception.ResourceNotFoundException;
import io.probestack.onboarding.model.AuditAction;
import io.probestack.onboarding.model.BusinessUnit;
import io.probestack.onboarding.model.BusinessUnitStatus;
import io.probestack.onboarding.model.OnboardingApplication;
import io.probestack.onboarding.model.OnboardingProject;
import io.probestack.onboarding.model.ProjectStatus;
import io.probestack.onboarding.model.ProjectEnvironment;
import io.probestack.onboarding.model.ResourceType;
import io.probestack.onboarding.repository.ApplicationConsumerLinkRepository;
import io.probestack.onboarding.repository.ApplicationRepository;
import io.probestack.onboarding.repository.ProjectRepository;
import io.probestack.onboarding.util.ActorResolver;
import io.probestack.onboarding.util.FieldChangeDetector;
import io.probestack.onboarding.util.SlugNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationConsumerLinkRepository linkRepository;
    private final BusinessUnitService businessUnitService;
    private final AuditService auditService;
    private final PagingService pagingService;
    private final AccessControlService accessControlService;

    public ProjectService(ProjectRepository projectRepository, ApplicationRepository applicationRepository,
                          ApplicationConsumerLinkRepository linkRepository, BusinessUnitService businessUnitService,
                          AuditService auditService, PagingService pagingService, AccessControlService accessControlService) {
        this.projectRepository = projectRepository;
        this.applicationRepository = applicationRepository;
        this.linkRepository = linkRepository;
        this.businessUnitService = businessUnitService;
        this.auditService = auditService;
        this.pagingService = pagingService;
        this.accessControlService = accessControlService;
    }

    public ProjectResponse create(String organizationId, ProjectCreateRequest request, ActorResolver.Actor actor) {
        BusinessUnit businessUnit = businessUnitService.find(organizationId, request.getBusinessUnitId());
        accessControlService.requireBusinessUnitManage(organizationId, businessUnit.getId(), actor);
        requireActiveBusinessUnit(businessUnit);
        String code = SlugNormalizer.normalizeCode(request.getCode());
        if (projectRepository.existsByOrganizationIdAndBusinessUnitIdAndCode(organizationId, businessUnit.getId(), code)) {
            throw new DuplicateResourceException("Project code already exists under this business unit: " + code);
        }
        OnboardingProject project = OnboardingProject.builder()
                .organizationId(organizationId)
                .businessUnitId(businessUnit.getId())
                .name(request.getName().trim())
                .code(code)
                .description(SlugNormalizer.trimToNull(request.getDescription()))
                .projectType(SlugNormalizer.trimToNull(request.getProjectType()))
                .portfolio(SlugNormalizer.trimToNull(request.getPortfolio()))
                .ownerName(SlugNormalizer.trimToNull(request.getOwnerName()))
                .ownerEmail(SlugNormalizer.trimToNull(request.getOwnerEmail()))
                .projectDlEmail(SlugNormalizer.trimToNull(request.getProjectDlEmail()))
                .projectManagerId(SlugNormalizer.trimToNull(request.getProjectManagerId()))
                .productManagerId(SlugNormalizer.trimToNull(request.getProductManagerId()))
                .scrumMasterId(SlugNormalizer.trimToNull(request.getScrumMasterId()))
                .technicalLeadId(SlugNormalizer.trimToNull(request.getTechnicalLeadId()))
                .securityLeadId(SlugNormalizer.trimToNull(request.getSecurityLeadId()))
                .devopsLeadId(SlugNormalizer.trimToNull(request.getDevopsLeadId()))
                .expectedGoLiveDate(request.getExpectedGoLiveDate())
                .deliveryModel(SlugNormalizer.trimToNull(request.getDeliveryModel()))
                .methodology(SlugNormalizer.trimToNull(request.getMethodology()))
                .sprintDuration(SlugNormalizer.trimToNull(request.getSprintDuration()))
                .repository(SlugNormalizer.trimToNull(request.getRepository()))
                .cicdTool(SlugNormalizer.trimToNull(request.getCicdTool()))
                .issueTracker(SlugNormalizer.trimToNull(request.getIssueTracker()))
                .documentationUrl(SlugNormalizer.trimToNull(request.getDocumentationUrl()))
                .authenticationMethod(SlugNormalizer.trimToNull(request.getAuthenticationMethod()))
                .authorizationMethod(SlugNormalizer.trimToNull(request.getAuthorizationMethod()))
                .oauthProvider(SlugNormalizer.trimToNull(request.getOauthProvider()))
                .mtlsEnabled(request.isMtlsEnabled())
                .jwtEnabled(request.isJwtEnabled())
                .apiKeyEnabled(request.isApiKeyEnabled())
                .secretsVault(SlugNormalizer.trimToNull(request.getSecretsVault()))
                .environments(normalizeEnvironments(request.getEnvironments()))
                .pciApplicable(request.isPciApplicable())
                .standardRules(SlugNormalizer.trimToNull(request.getStandardRules()))
                .customRules(SlugNormalizer.trimToNull(request.getCustomRules()))
                .owaspTop10Enabled(request.isOwaspTop10Enabled())
                .lintingEnabled(request.isLintingEnabled())
                .status(request.getStatus() == null ? ProjectStatus.READY : request.getStatus())
                .createdBy(actor.name())
                .createdByEmail(actor.email())
                .updatedBy(actor.name())
                .updatedByEmail(actor.email())
                .build();
        OnboardingProject saved = projectRepository.save(project);
        auditService.record(organizationId, ResourceType.PROJECT, saved.getId(), AuditAction.CREATE, actor, List.of(), null, saved);
        return toResponse(saved);
    }

    public PagedResult<ProjectResponse> list(String organizationId, String businessUnitId, String search, ProjectStatus status, int page, int size, ActorResolver.Actor actor) {
        List<OnboardingProject> source = StringUtils.hasText(businessUnitId)
                ? projectRepository.findByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, businessUnitId)
                : projectRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId);
        List<ProjectResponse> filtered = accessControlService.filterProjects(organizationId, source, actor).stream()
                .filter(project -> status == null || project.getStatus() == status)
                .filter(project -> matches(search, project.getName(), project.getCode(), project.getOwnerName()))
                .map(this::toResponse)
                .toList();
        return pagingService.page(filtered, page, size);
    }

    public ProjectResponse get(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireProjectView(organizationId, id, actor);
        return toResponse(find(organizationId, id));
    }

    public List<ApplicationSummaryResponse> applications(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireProjectView(organizationId, id, actor);
        find(organizationId, id);
        return accessControlService.filterApplications(organizationId, applicationRepository.findByOrganizationIdAndProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, id), actor)
                .stream().map(this::toApplicationSummary).toList();
    }

    public ProjectResponse update(String organizationId, String id, ProjectUpdateRequest request, ActorResolver.Actor actor) {
        accessControlService.requireProjectManage(organizationId, id, actor);
        OnboardingProject project = find(organizationId, id);
        Map<String, Object> before = projectFields(project);
        String originalBusinessUnitId = project.getBusinessUnitId();
        if (StringUtils.hasText(request.getBusinessUnitId()) && !request.getBusinessUnitId().equals(originalBusinessUnitId)) {
            BusinessUnit businessUnit = businessUnitService.find(organizationId, request.getBusinessUnitId());
            accessControlService.requireBusinessUnitManage(organizationId, businessUnit.getId(), actor);
            requireActiveBusinessUnit(businessUnit);
            project.setBusinessUnitId(businessUnit.getId());
        }
        String nextCode = StringUtils.hasText(request.getCode()) ? SlugNormalizer.normalizeCode(request.getCode()) : project.getCode();
        if ((!project.getBusinessUnitId().equals(originalBusinessUnitId) || !nextCode.equals(project.getCode()))
                && projectRepository.existsByOrganizationIdAndBusinessUnitIdAndCode(organizationId, project.getBusinessUnitId(), nextCode)) {
            throw new DuplicateResourceException("Project code already exists under this business unit: " + nextCode);
        }
        if (StringUtils.hasText(request.getCode())) {
            project.setCode(nextCode);
        }
        if (StringUtils.hasText(request.getName())) project.setName(request.getName().trim());
        if (request.getDescription() != null) project.setDescription(SlugNormalizer.trimToNull(request.getDescription()));
        if (request.getProjectType() != null) project.setProjectType(SlugNormalizer.trimToNull(request.getProjectType()));
        if (request.getPortfolio() != null) project.setPortfolio(SlugNormalizer.trimToNull(request.getPortfolio()));
        if (request.getOwnerName() != null) project.setOwnerName(SlugNormalizer.trimToNull(request.getOwnerName()));
        if (request.getOwnerEmail() != null) project.setOwnerEmail(SlugNormalizer.trimToNull(request.getOwnerEmail()));
        if (request.getProjectDlEmail() != null) project.setProjectDlEmail(SlugNormalizer.trimToNull(request.getProjectDlEmail()));
        if (request.getProjectManagerId() != null) project.setProjectManagerId(SlugNormalizer.trimToNull(request.getProjectManagerId()));
        if (request.getProductManagerId() != null) project.setProductManagerId(SlugNormalizer.trimToNull(request.getProductManagerId()));
        if (request.getScrumMasterId() != null) project.setScrumMasterId(SlugNormalizer.trimToNull(request.getScrumMasterId()));
        if (request.getTechnicalLeadId() != null) project.setTechnicalLeadId(SlugNormalizer.trimToNull(request.getTechnicalLeadId()));
        if (request.getSecurityLeadId() != null) project.setSecurityLeadId(SlugNormalizer.trimToNull(request.getSecurityLeadId()));
        if (request.getDevopsLeadId() != null) project.setDevopsLeadId(SlugNormalizer.trimToNull(request.getDevopsLeadId()));
        if (request.getExpectedGoLiveDate() != null) project.setExpectedGoLiveDate(request.getExpectedGoLiveDate());
        if (request.getDeliveryModel() != null) project.setDeliveryModel(SlugNormalizer.trimToNull(request.getDeliveryModel()));
        if (request.getMethodology() != null) project.setMethodology(SlugNormalizer.trimToNull(request.getMethodology()));
        if (request.getSprintDuration() != null) project.setSprintDuration(SlugNormalizer.trimToNull(request.getSprintDuration()));
        if (request.getRepository() != null) project.setRepository(SlugNormalizer.trimToNull(request.getRepository()));
        if (request.getCicdTool() != null) project.setCicdTool(SlugNormalizer.trimToNull(request.getCicdTool()));
        if (request.getIssueTracker() != null) project.setIssueTracker(SlugNormalizer.trimToNull(request.getIssueTracker()));
        if (request.getDocumentationUrl() != null) project.setDocumentationUrl(SlugNormalizer.trimToNull(request.getDocumentationUrl()));
        if (request.getAuthenticationMethod() != null) project.setAuthenticationMethod(SlugNormalizer.trimToNull(request.getAuthenticationMethod()));
        if (request.getAuthorizationMethod() != null) project.setAuthorizationMethod(SlugNormalizer.trimToNull(request.getAuthorizationMethod()));
        if (request.getOauthProvider() != null) project.setOauthProvider(SlugNormalizer.trimToNull(request.getOauthProvider()));
        if (request.getMtlsEnabled() != null) project.setMtlsEnabled(request.getMtlsEnabled());
        if (request.getJwtEnabled() != null) project.setJwtEnabled(request.getJwtEnabled());
        if (request.getApiKeyEnabled() != null) project.setApiKeyEnabled(request.getApiKeyEnabled());
        if (request.getSecretsVault() != null) project.setSecretsVault(SlugNormalizer.trimToNull(request.getSecretsVault()));
        if (request.getEnvironments() != null) project.setEnvironments(normalizeEnvironments(request.getEnvironments()));
        if (request.getPciApplicable() != null) project.setPciApplicable(request.getPciApplicable());
        if (request.getStandardRules() != null) project.setStandardRules(SlugNormalizer.trimToNull(request.getStandardRules()));
        if (request.getCustomRules() != null) project.setCustomRules(SlugNormalizer.trimToNull(request.getCustomRules()));
        if (request.getOwaspTop10Enabled() != null) project.setOwaspTop10Enabled(request.getOwaspTop10Enabled());
        if (request.getLintingEnabled() != null) project.setLintingEnabled(request.getLintingEnabled());
        if (request.getStatus() != null) project.setStatus(request.getStatus());
        project.setUpdatedBy(actor.name());
        project.setUpdatedByEmail(actor.email());
        OnboardingProject saved = projectRepository.save(project);
        var changes = FieldChangeDetector.diff(before, projectFields(saved));
        auditService.record(organizationId, ResourceType.PROJECT, id, statusChanged(before, saved.getStatus()) ? AuditAction.STATUS_CHANGE : AuditAction.UPDATE, actor, changes, before, saved);
        return toResponse(saved);
    }

    public void delete(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireProjectManage(organizationId, id, actor);
        OnboardingProject project = find(organizationId, id);
        Map<String, Object> before = projectFields(project);
        project.setStatus(ProjectStatus.DELETED);
        project.setDeletedAt(Instant.now());
        project.setDeletedBy(actor.name());
        project.setDeletedByEmail(actor.email());
        project.setUpdatedBy(actor.name());
        project.setUpdatedByEmail(actor.email());
        OnboardingProject saved = projectRepository.save(project);
        auditService.record(organizationId, ResourceType.PROJECT, id, AuditAction.DELETE, actor, FieldChangeDetector.diff(before, projectFields(saved)), before, saved);
    }

    public List<AuditLogResponse> history(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireProjectView(organizationId, id, actor);
        find(organizationId, id);
        return auditService.history(organizationId, ResourceType.PROJECT, id);
    }

    public OnboardingProject find(String organizationId, String id) {
        return projectRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    public ProjectResponse toResponse(OnboardingProject project) {
        String businessUnitName = businessUnitService.find(project.getOrganizationId(), project.getBusinessUnitId()).getName();
        return ProjectResponse.builder()
                .id(project.getId())
                .organizationId(project.getOrganizationId())
                .businessUnitId(project.getBusinessUnitId())
                .businessUnitName(businessUnitName)
                .name(project.getName())
                .code(project.getCode())
                .description(project.getDescription())
                .projectType(project.getProjectType())
                .portfolio(project.getPortfolio())
                .ownerName(project.getOwnerName())
                .ownerEmail(project.getOwnerEmail())
                .projectDlEmail(project.getProjectDlEmail())
                .projectManagerId(project.getProjectManagerId())
                .productManagerId(project.getProductManagerId())
                .scrumMasterId(project.getScrumMasterId())
                .technicalLeadId(project.getTechnicalLeadId())
                .securityLeadId(project.getSecurityLeadId())
                .devopsLeadId(project.getDevopsLeadId())
                .expectedGoLiveDate(project.getExpectedGoLiveDate())
                .deliveryModel(project.getDeliveryModel())
                .methodology(project.getMethodology())
                .sprintDuration(project.getSprintDuration())
                .repository(project.getRepository())
                .cicdTool(project.getCicdTool())
                .issueTracker(project.getIssueTracker())
                .documentationUrl(project.getDocumentationUrl())
                .authenticationMethod(project.getAuthenticationMethod())
                .authorizationMethod(project.getAuthorizationMethod())
                .oauthProvider(project.getOauthProvider())
                .mtlsEnabled(project.isMtlsEnabled())
                .jwtEnabled(project.isJwtEnabled())
                .apiKeyEnabled(project.isApiKeyEnabled())
                .secretsVault(project.getSecretsVault())
                .environments(project.getEnvironments() == null ? List.of() : project.getEnvironments())
                .pciApplicable(project.isPciApplicable())
                .standardRules(project.getStandardRules())
                .customRules(project.getCustomRules())
                .owaspTop10Enabled(project.isOwaspTop10Enabled())
                .lintingEnabled(project.isLintingEnabled())
                .status(project.getStatus())
                .applicationCount(applicationRepository.countByOrganizationIdAndProjectIdAndDeletedAtIsNull(project.getOrganizationId(), project.getId()))
                .createdBy(project.getCreatedBy())
                .createdByEmail(project.getCreatedByEmail())
                .updatedBy(project.getUpdatedBy())
                .updatedByEmail(project.getUpdatedByEmail())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ApplicationSummaryResponse toApplicationSummary(OnboardingApplication app) {
        return ApplicationSummaryResponse.builder()
                .id(app.getId())
                .name(app.getName())
                .applicationId(app.getApplicationId())
                .ownerName(app.getOwnerName())
                .status(app.getStatus())
                .consumerCount(linkRepository.countByOrganizationIdAndApplicationId(app.getOrganizationId(), app.getId()))
                .build();
    }

    private void requireActiveBusinessUnit(BusinessUnit businessUnit) {
        if (businessUnit.getStatus() != BusinessUnitStatus.ACTIVE) {
            throw new InvalidStatusTransitionException("Project onboarding requires an active business unit");
        }
    }

    private boolean matches(String search, String... values) {
        if (!StringUtils.hasText(search)) return true;
        String needle = search.trim().toLowerCase();
        for (String value : values) {
            if (value != null && value.toLowerCase().contains(needle)) return true;
        }
        return false;
    }

    private boolean statusChanged(Map<String, Object> before, ProjectStatus status) {
        return !String.valueOf(before.get("status")).equals(String.valueOf(status));
    }

    private Map<String, Object> projectFields(OnboardingProject project) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("businessUnitId", value(project.getBusinessUnitId()));
        fields.put("name", value(project.getName()));
        fields.put("code", value(project.getCode()));
        fields.put("description", value(project.getDescription()));
        fields.put("projectType", value(project.getProjectType()));
        fields.put("portfolio", value(project.getPortfolio()));
        fields.put("ownerName", value(project.getOwnerName()));
        fields.put("ownerEmail", value(project.getOwnerEmail()));
        fields.put("projectDlEmail", value(project.getProjectDlEmail()));
        fields.put("projectManagerId", value(project.getProjectManagerId()));
        fields.put("productManagerId", value(project.getProductManagerId()));
        fields.put("scrumMasterId", value(project.getScrumMasterId()));
        fields.put("technicalLeadId", value(project.getTechnicalLeadId()));
        fields.put("securityLeadId", value(project.getSecurityLeadId()));
        fields.put("devopsLeadId", value(project.getDevopsLeadId()));
        fields.put("expectedGoLiveDate", value(project.getExpectedGoLiveDate()));
        fields.put("deliveryModel", value(project.getDeliveryModel()));
        fields.put("methodology", value(project.getMethodology()));
        fields.put("sprintDuration", value(project.getSprintDuration()));
        fields.put("repository", value(project.getRepository()));
        fields.put("cicdTool", value(project.getCicdTool()));
        fields.put("issueTracker", value(project.getIssueTracker()));
        fields.put("documentationUrl", value(project.getDocumentationUrl()));
        fields.put("authenticationMethod", value(project.getAuthenticationMethod()));
        fields.put("authorizationMethod", value(project.getAuthorizationMethod()));
        fields.put("oauthProvider", value(project.getOauthProvider()));
        fields.put("mtlsEnabled", project.isMtlsEnabled());
        fields.put("jwtEnabled", project.isJwtEnabled());
        fields.put("apiKeyEnabled", project.isApiKeyEnabled());
        fields.put("secretsVault", value(project.getSecretsVault()));
        fields.put("environments", project.getEnvironments() == null ? List.of() : project.getEnvironments());
        fields.put("pciApplicable", project.isPciApplicable());
        fields.put("standardRules", value(project.getStandardRules()));
        fields.put("customRules", value(project.getCustomRules()));
        fields.put("owaspTop10Enabled", project.isOwaspTop10Enabled());
        fields.put("lintingEnabled", project.isLintingEnabled());
        fields.put("status", project.getStatus());
        return fields;
    }

    private List<ProjectEnvironment> normalizeEnvironments(List<ProjectEnvironment> requested) {
        if (requested == null) return new ArrayList<>();
        Set<String> types = new HashSet<>();
        List<ProjectEnvironment> normalized = new ArrayList<>();
        for (ProjectEnvironment environment : requested) {
            String type = SlugNormalizer.trimToNull(environment.getEnvironmentType());
            if (type == null || !types.add(type)) throw new IllegalArgumentException("Each environment type may be configured only once");
            normalized.add(ProjectEnvironment.builder()
                    .environmentType(type)
                    .endpointUrl(SlugNormalizer.trimToNull(environment.getEndpointUrl()))
                    .enabled(environment.isEnabled())
                    .build());
        }
        return normalized;
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }
}

