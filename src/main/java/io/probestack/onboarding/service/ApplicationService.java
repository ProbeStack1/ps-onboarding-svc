package io.probestack.onboarding.service;

import io.probestack.onboarding.dto.application.ApplicationConsumersRequest;
import io.probestack.onboarding.dto.application.ApplicationCreateRequest;
import io.probestack.onboarding.dto.application.ApplicationResponse;
import io.probestack.onboarding.dto.application.ApplicationUpdateRequest;
import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.exception.DuplicateResourceException;
import io.probestack.onboarding.exception.OrganizationMismatchException;
import io.probestack.onboarding.exception.ResourceNotFoundException;
import io.probestack.onboarding.model.ApplicationConsumerLink;
import io.probestack.onboarding.model.ApplicationStatus;
import io.probestack.onboarding.model.AuditAction;
import io.probestack.onboarding.model.BusinessUnit;
import io.probestack.onboarding.model.Consumer;
import io.probestack.onboarding.model.OnboardingApplication;
import io.probestack.onboarding.model.OnboardingProject;
import io.probestack.onboarding.model.ResourceType;
import io.probestack.onboarding.repository.ApplicationConsumerLinkRepository;
import io.probestack.onboarding.repository.ApplicationRepository;
import io.probestack.onboarding.repository.ConsumerRepository;
import io.probestack.onboarding.util.ActorResolver;
import io.probestack.onboarding.util.FieldChangeDetector;
import io.probestack.onboarding.util.SlugNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final ApplicationConsumerLinkRepository linkRepository;
    private final ConsumerRepository consumerRepository;
    private final BusinessUnitService businessUnitService;
    private final ProjectService projectService;
    private final AuditService auditService;
    private final PagingService pagingService;
    private final AccessControlService accessControlService;

    public ApplicationService(ApplicationRepository applicationRepository, ApplicationConsumerLinkRepository linkRepository,
                              ConsumerRepository consumerRepository, BusinessUnitService businessUnitService,
                              ProjectService projectService, AuditService auditService, PagingService pagingService, AccessControlService accessControlService) {
        this.applicationRepository = applicationRepository;
        this.linkRepository = linkRepository;
        this.consumerRepository = consumerRepository;
        this.businessUnitService = businessUnitService;
        this.projectService = projectService;
        this.auditService = auditService;
        this.pagingService = pagingService;
        this.accessControlService = accessControlService;
    }

    public ApplicationResponse create(String organizationId, ApplicationCreateRequest request, ActorResolver.Actor actor) {
        BusinessUnit businessUnit = businessUnitService.find(organizationId, request.getBusinessUnitId());
        OnboardingProject project = projectService.find(organizationId, request.getProjectId());
        accessControlService.requireProjectManage(organizationId, project.getId(), actor);
        requireProjectUnderBusinessUnit(project, businessUnit.getId());
        String applicationId = SlugNormalizer.normalizeCode(request.getApplicationId());
        if (applicationRepository.existsByOrganizationIdAndApplicationId(organizationId, applicationId)) {
            throw new DuplicateResourceException("Application id already exists for this organization: " + applicationId);
        }
        OnboardingApplication application = OnboardingApplication.builder()
                .organizationId(organizationId)
                .businessUnitId(businessUnit.getId())
                .projectId(project.getId())
                .name(request.getName().trim())
                .applicationId(applicationId)
                .ownerName(SlugNormalizer.trimToNull(request.getOwnerName()))
                .ownerEmail(SlugNormalizer.trimToNull(request.getOwnerEmail()))
                .applicationSme(SlugNormalizer.trimToNull(request.getApplicationSme()))
                .smeEmail(SlugNormalizer.trimToNull(request.getSmeEmail()))
                .testerName(SlugNormalizer.trimToNull(request.getTesterName()))
                .testerEmail(SlugNormalizer.trimToNull(request.getTesterEmail()))
                .serviceNowGroupName(SlugNormalizer.trimToNull(request.getServiceNowGroupName()))
                .serviceNowEmail(SlugNormalizer.trimToNull(request.getServiceNowEmail()))
                .status(request.getStatus() == null ? ApplicationStatus.DRAFT : request.getStatus())
                .createdBy(actor.name())
                .createdByEmail(actor.email())
                .updatedBy(actor.name())
                .updatedByEmail(actor.email())
                .build();
        OnboardingApplication saved = applicationRepository.save(application);
        auditService.record(organizationId, ResourceType.APPLICATION, saved.getId(), AuditAction.CREATE, actor, List.of(), null, saved);
        replaceConsumers(organizationId, saved.getId(), request.getConsumerIds(), actor);
        return toResponse(saved);
    }

    public PagedResult<ApplicationResponse> list(String organizationId, String businessUnitId, String projectId, String search, ApplicationStatus status, int page, int size, ActorResolver.Actor actor) {
        List<OnboardingApplication> source;
        if (StringUtils.hasText(projectId)) {
            source = applicationRepository.findByOrganizationIdAndProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, projectId);
        } else if (StringUtils.hasText(businessUnitId)) {
            source = applicationRepository.findByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, businessUnitId);
        } else if (status != null) {
            source = applicationRepository.findByOrganizationIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, status);
        } else {
            source = applicationRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId);
        }
        List<ApplicationResponse> filtered = accessControlService.filterApplications(organizationId, source, actor).stream()
                .filter(app -> status == null || app.getStatus() == status)
                .filter(app -> matches(search, app.getName(), app.getApplicationId(), app.getOwnerName()))
                .map(this::toResponse)
                .toList();
        return pagingService.page(filtered, page, size);
    }

    public ApplicationResponse get(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireApplicationView(organizationId, id, actor);
        return toResponse(find(organizationId, id));
    }

    public ApplicationResponse update(String organizationId, String id, ApplicationUpdateRequest request, ActorResolver.Actor actor) {
        accessControlService.requireApplicationManage(organizationId, id, actor);
        OnboardingApplication app = find(organizationId, id);
        Map<String, Object> before = applicationFields(app);
        String nextBusinessUnitId = StringUtils.hasText(request.getBusinessUnitId()) ? request.getBusinessUnitId() : app.getBusinessUnitId();
        String nextProjectId = StringUtils.hasText(request.getProjectId()) ? request.getProjectId() : app.getProjectId();
        BusinessUnit businessUnit = businessUnitService.find(organizationId, nextBusinessUnitId);
        OnboardingProject project = projectService.find(organizationId, nextProjectId);
        accessControlService.requireProjectManage(organizationId, project.getId(), actor);
        requireProjectUnderBusinessUnit(project, businessUnit.getId());
        app.setBusinessUnitId(businessUnit.getId());
        app.setProjectId(project.getId());
        if (StringUtils.hasText(request.getApplicationId())) {
            String applicationId = SlugNormalizer.normalizeCode(request.getApplicationId());
            if (!applicationId.equals(app.getApplicationId()) && applicationRepository.existsByOrganizationIdAndApplicationId(organizationId, applicationId)) {
                throw new DuplicateResourceException("Application id already exists for this organization: " + applicationId);
            }
            app.setApplicationId(applicationId);
        }
        if (StringUtils.hasText(request.getName())) app.setName(request.getName().trim());
        if (request.getOwnerName() != null) app.setOwnerName(SlugNormalizer.trimToNull(request.getOwnerName()));
        if (request.getOwnerEmail() != null) app.setOwnerEmail(SlugNormalizer.trimToNull(request.getOwnerEmail()));
        if (request.getApplicationSme() != null) app.setApplicationSme(SlugNormalizer.trimToNull(request.getApplicationSme()));
        if (request.getSmeEmail() != null) app.setSmeEmail(SlugNormalizer.trimToNull(request.getSmeEmail()));
        if (request.getTesterName() != null) app.setTesterName(SlugNormalizer.trimToNull(request.getTesterName()));
        if (request.getTesterEmail() != null) app.setTesterEmail(SlugNormalizer.trimToNull(request.getTesterEmail()));
        if (request.getServiceNowGroupName() != null) app.setServiceNowGroupName(SlugNormalizer.trimToNull(request.getServiceNowGroupName()));
        if (request.getServiceNowEmail() != null) app.setServiceNowEmail(SlugNormalizer.trimToNull(request.getServiceNowEmail()));
        if (request.getStatus() != null) app.setStatus(request.getStatus());
        app.setUpdatedBy(actor.name());
        app.setUpdatedByEmail(actor.email());
        OnboardingApplication saved = applicationRepository.save(app);
        var changes = FieldChangeDetector.diff(before, applicationFields(saved));
        auditService.record(organizationId, ResourceType.APPLICATION, id, statusChanged(before, saved.getStatus()) ? AuditAction.STATUS_CHANGE : AuditAction.UPDATE, actor, changes, before, saved);
        if (request.getConsumerIds() != null) {
            replaceConsumers(organizationId, id, request.getConsumerIds(), actor);
        }
        return toResponse(saved);
    }

    public ApplicationResponse replaceConsumers(String organizationId, String applicationId, ApplicationConsumersRequest request, ActorResolver.Actor actor) {
        replaceConsumers(organizationId, applicationId, request.getConsumerIds(), actor);
        return get(organizationId, applicationId, actor);
    }

    public void replaceConsumers(String organizationId, String applicationId, List<String> consumerIds, ActorResolver.Actor actor) {
        accessControlService.requireApplicationManage(organizationId, applicationId, actor);
        find(organizationId, applicationId);
        List<String> requestedIds = consumerIds == null ? List.of() : consumerIds.stream().filter(StringUtils::hasText).distinct().toList();
        List<Consumer> consumers = requestedIds.isEmpty() ? List.of() : consumerRepository.findByOrganizationIdAndIdInAndDeletedAtIsNull(organizationId, requestedIds);
        if (consumers.size() != requestedIds.size()) {
            throw new OrganizationMismatchException("All selected consumers must exist in the same organization");
        }
        Set<String> existing = linkRepository.findByOrganizationIdAndApplicationId(organizationId, applicationId).stream().map(ApplicationConsumerLink::getConsumerId).collect(java.util.stream.Collectors.toSet());
        Set<String> requested = new HashSet<>(requestedIds);
        List<String> removed = existing.stream().filter(id -> !requested.contains(id)).toList();
        List<String> added = requested.stream().filter(id -> !existing.contains(id)).toList();
        if (!removed.isEmpty()) {
            linkRepository.deleteByOrganizationIdAndApplicationIdAndConsumerIdIn(organizationId, applicationId, removed);
            auditService.record(organizationId, ResourceType.APPLICATION, applicationId, AuditAction.UNLINK_CONSUMER, actor, List.of(), Map.of("consumerIds", removed), Map.of("consumerIds", requestedIds));
        }
        for (String consumerId : added) {
            linkRepository.save(ApplicationConsumerLink.builder()
                    .organizationId(organizationId)
                    .applicationId(applicationId)
                    .consumerId(consumerId)
                    .createdBy(actor.name())
                    .createdByEmail(actor.email())
                    .build());
        }
        if (!added.isEmpty()) {
            auditService.record(organizationId, ResourceType.APPLICATION, applicationId, AuditAction.LINK_CONSUMER, actor, List.of(), Map.of("consumerIds", existing), Map.of("consumerIds", requestedIds));
        }
    }

    public void delete(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireApplicationManage(organizationId, id, actor);
        OnboardingApplication app = find(organizationId, id);
        Map<String, Object> before = applicationFields(app);
        app.setStatus(ApplicationStatus.DELETED);
        app.setDeletedAt(Instant.now());
        app.setDeletedBy(actor.name());
        app.setDeletedByEmail(actor.email());
        app.setUpdatedBy(actor.name());
        app.setUpdatedByEmail(actor.email());
        OnboardingApplication saved = applicationRepository.save(app);
        auditService.record(organizationId, ResourceType.APPLICATION, id, AuditAction.DELETE, actor, FieldChangeDetector.diff(before, applicationFields(saved)), before, saved);
    }

    public List<AuditLogResponse> history(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireApplicationView(organizationId, id, actor);
        find(organizationId, id);
        return auditService.history(organizationId, ResourceType.APPLICATION, id);
    }

    public OnboardingApplication find(String organizationId, String id) {
        return applicationRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    public ApplicationResponse toResponse(OnboardingApplication app) {
        BusinessUnit bu = businessUnitService.find(app.getOrganizationId(), app.getBusinessUnitId());
        OnboardingProject project = projectService.find(app.getOrganizationId(), app.getProjectId());
        List<String> consumerIds = linkRepository.findByOrganizationIdAndApplicationId(app.getOrganizationId(), app.getId()).stream().map(ApplicationConsumerLink::getConsumerId).toList();
        return ApplicationResponse.builder()
                .id(app.getId())
                .organizationId(app.getOrganizationId())
                .businessUnitId(app.getBusinessUnitId())
                .businessUnitName(bu.getName())
                .projectId(app.getProjectId())
                .projectName(project.getName())
                .name(app.getName())
                .applicationId(app.getApplicationId())
                .ownerName(app.getOwnerName())
                .ownerEmail(app.getOwnerEmail())
                .applicationSme(app.getApplicationSme())
                .smeEmail(app.getSmeEmail())
                .testerName(app.getTesterName())
                .testerEmail(app.getTesterEmail())
                .serviceNowGroupName(app.getServiceNowGroupName())
                .serviceNowEmail(app.getServiceNowEmail())
                .status(app.getStatus())
                .consumerCount(consumerIds.size())
                .consumerIds(consumerIds)
                .createdBy(app.getCreatedBy())
                .createdByEmail(app.getCreatedByEmail())
                .updatedBy(app.getUpdatedBy())
                .updatedByEmail(app.getUpdatedByEmail())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private void requireProjectUnderBusinessUnit(OnboardingProject project, String businessUnitId) {
        if (!project.getBusinessUnitId().equals(businessUnitId)) {
            throw new OrganizationMismatchException("Project does not belong to the selected business unit");
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

    private boolean statusChanged(Map<String, Object> before, ApplicationStatus status) {
        return !String.valueOf(before.get("status")).equals(String.valueOf(status));
    }

    private Map<String, Object> applicationFields(OnboardingApplication app) {
        return Map.ofEntries(
                Map.entry("businessUnitId", nullToEmpty(app.getBusinessUnitId())),
                Map.entry("projectId", nullToEmpty(app.getProjectId())),
                Map.entry("name", nullToEmpty(app.getName())),
                Map.entry("applicationId", nullToEmpty(app.getApplicationId())),
                Map.entry("ownerName", nullToEmpty(app.getOwnerName())),
                Map.entry("ownerEmail", nullToEmpty(app.getOwnerEmail())),
                Map.entry("applicationSme", nullToEmpty(app.getApplicationSme())),
                Map.entry("smeEmail", nullToEmpty(app.getSmeEmail())),
                Map.entry("testerName", nullToEmpty(app.getTesterName())),
                Map.entry("testerEmail", nullToEmpty(app.getTesterEmail())),
                Map.entry("serviceNowGroupName", nullToEmpty(app.getServiceNowGroupName())),
                Map.entry("serviceNowEmail", nullToEmpty(app.getServiceNowEmail())),
                Map.entry("status", app.getStatus())
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

