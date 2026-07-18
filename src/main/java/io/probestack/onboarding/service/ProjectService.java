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
import java.util.List;
import java.util.Map;

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
                .ownerName(SlugNormalizer.trimToNull(request.getOwnerName()))
                .ownerEmail(SlugNormalizer.trimToNull(request.getOwnerEmail()))
                .projectDlEmail(SlugNormalizer.trimToNull(request.getProjectDlEmail()))
                .expectedGoLiveDate(request.getExpectedGoLiveDate())
                .deliveryModel(SlugNormalizer.trimToNull(request.getDeliveryModel()))
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
        if (StringUtils.hasText(request.getBusinessUnitId()) && !request.getBusinessUnitId().equals(project.getBusinessUnitId())) {
            BusinessUnit businessUnit = businessUnitService.find(organizationId, request.getBusinessUnitId());
            requireActiveBusinessUnit(businessUnit);
            project.setBusinessUnitId(businessUnit.getId());
        }
        if (StringUtils.hasText(request.getCode())) {
            String code = SlugNormalizer.normalizeCode(request.getCode());
            if (!code.equals(project.getCode()) && projectRepository.existsByOrganizationIdAndBusinessUnitIdAndCode(organizationId, project.getBusinessUnitId(), code)) {
                throw new DuplicateResourceException("Project code already exists under this business unit: " + code);
            }
            project.setCode(code);
        }
        if (StringUtils.hasText(request.getName())) project.setName(request.getName().trim());
        if (request.getOwnerName() != null) project.setOwnerName(SlugNormalizer.trimToNull(request.getOwnerName()));
        if (request.getOwnerEmail() != null) project.setOwnerEmail(SlugNormalizer.trimToNull(request.getOwnerEmail()));
        if (request.getProjectDlEmail() != null) project.setProjectDlEmail(SlugNormalizer.trimToNull(request.getProjectDlEmail()));
        if (request.getExpectedGoLiveDate() != null) project.setExpectedGoLiveDate(request.getExpectedGoLiveDate());
        if (request.getDeliveryModel() != null) project.setDeliveryModel(SlugNormalizer.trimToNull(request.getDeliveryModel()));
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
                .ownerName(project.getOwnerName())
                .ownerEmail(project.getOwnerEmail())
                .projectDlEmail(project.getProjectDlEmail())
                .expectedGoLiveDate(project.getExpectedGoLiveDate())
                .deliveryModel(project.getDeliveryModel())
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
        return Map.of(
                "businessUnitId", nullToEmpty(project.getBusinessUnitId()),
                "name", nullToEmpty(project.getName()),
                "code", nullToEmpty(project.getCode()),
                "ownerName", nullToEmpty(project.getOwnerName()),
                "ownerEmail", nullToEmpty(project.getOwnerEmail()),
                "projectDlEmail", nullToEmpty(project.getProjectDlEmail()),
                "expectedGoLiveDate", project.getExpectedGoLiveDate() == null ? "" : project.getExpectedGoLiveDate().toString(),
                "deliveryModel", nullToEmpty(project.getDeliveryModel()),
                "status", project.getStatus()
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

