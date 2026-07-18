package io.probestack.onboarding.service;

import io.probestack.onboarding.dto.application.ApplicationSummaryResponse;
import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.businessunit.BusinessUnitCreateRequest;
import io.probestack.onboarding.dto.businessunit.BusinessUnitResponse;
import io.probestack.onboarding.dto.businessunit.BusinessUnitTreeResponse;
import io.probestack.onboarding.dto.businessunit.BusinessUnitUpdateRequest;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.dto.project.ProjectTreeNode;
import io.probestack.onboarding.exception.DuplicateResourceException;
import io.probestack.onboarding.exception.ResourceNotFoundException;
import io.probestack.onboarding.model.AuditAction;
import io.probestack.onboarding.model.BusinessUnit;
import io.probestack.onboarding.model.BusinessUnitStatus;
import io.probestack.onboarding.model.OnboardingApplication;
import io.probestack.onboarding.model.OnboardingProject;
import io.probestack.onboarding.model.ResourceType;
import io.probestack.onboarding.repository.ApplicationConsumerLinkRepository;
import io.probestack.onboarding.repository.ApplicationRepository;
import io.probestack.onboarding.repository.BusinessUnitRepository;
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
public class BusinessUnitService {
    private final BusinessUnitRepository businessUnitRepository;
    private final ProjectRepository projectRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationConsumerLinkRepository linkRepository;
    private final AuditService auditService;
    private final PagingService pagingService;
    private final AccessControlService accessControlService;

    public BusinessUnitService(BusinessUnitRepository businessUnitRepository, ProjectRepository projectRepository,
                               ApplicationRepository applicationRepository, ApplicationConsumerLinkRepository linkRepository,
                               AuditService auditService, PagingService pagingService, AccessControlService accessControlService) {
        this.businessUnitRepository = businessUnitRepository;
        this.projectRepository = projectRepository;
        this.applicationRepository = applicationRepository;
        this.linkRepository = linkRepository;
        this.auditService = auditService;
        this.pagingService = pagingService;
        this.accessControlService = accessControlService;
    }

    public BusinessUnitResponse create(String organizationId, BusinessUnitCreateRequest request, ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        String code = SlugNormalizer.normalizeCode(request.getCode());
        if (businessUnitRepository.existsByOrganizationIdAndCode(organizationId, code)) {
            throw new DuplicateResourceException("Business unit code already exists for this organization: " + code);
        }
        BusinessUnit businessUnit = BusinessUnit.builder()
                .organizationId(organizationId)
                .name(request.getName().trim())
                .code(code)
                .ownerName(SlugNormalizer.trimToNull(request.getOwnerName()))
                .ownerEmail(SlugNormalizer.trimToNull(request.getOwnerEmail()))
                .costCenter(SlugNormalizer.trimToNull(request.getCostCenter()))
                .description(SlugNormalizer.trimToNull(request.getDescription()))
                .status(request.getStatus() == null ? BusinessUnitStatus.ACTIVE : request.getStatus())
                .createdBy(actor.name())
                .createdByEmail(actor.email())
                .updatedBy(actor.name())
                .updatedByEmail(actor.email())
                .build();
        BusinessUnit saved = businessUnitRepository.save(businessUnit);
        auditService.record(organizationId, ResourceType.BUSINESS_UNIT, saved.getId(), AuditAction.CREATE, actor, List.of(), null, saved);
        return toResponse(saved);
    }

    public PagedResult<BusinessUnitResponse> list(String organizationId, String search, BusinessUnitStatus status, int page, int size, ActorResolver.Actor actor) {
        List<BusinessUnit> source = status == null
                ? businessUnitRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId)
                : businessUnitRepository.findByOrganizationIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, status);
        List<BusinessUnitResponse> filtered = accessControlService.filterBusinessUnits(organizationId, source, actor).stream()
                .filter(item -> matches(search, item.getName(), item.getCode(), item.getOwnerName()))
                .map(this::toResponse)
                .toList();
        return pagingService.page(filtered, page, size);
    }

    public BusinessUnitResponse get(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireBusinessUnitView(organizationId, id, actor);
        return toResponse(find(organizationId, id));
    }

    public BusinessUnitTreeResponse tree(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireBusinessUnitView(organizationId, id, actor);
        BusinessUnit businessUnit = find(organizationId, id);
        List<ProjectTreeNode> nodes = accessControlService.filterProjects(organizationId, projectRepository.findByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, id), actor)
                .stream()
                .map(project -> {
                    List<ApplicationSummaryResponse> apps = accessControlService.filterApplications(organizationId, applicationRepository.findByOrganizationIdAndProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, project.getId()), actor)
                            .stream().map(this::toApplicationSummary).toList();
                    return ProjectTreeNode.builder()
                            .id(project.getId())
                            .name(project.getName())
                            .code(project.getCode())
                            .ownerName(project.getOwnerName())
                            .expectedGoLiveDate(project.getExpectedGoLiveDate())
                            .status(project.getStatus())
                            .applicationCount(apps.size())
                            .applications(apps)
                            .build();
                })
                .toList();
        return BusinessUnitTreeResponse.builder()
                .businessUnit(toResponse(businessUnit))
                .projectCount(nodes.size())
                .applicationCount(applicationRepository.countByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNull(organizationId, id))
                .projects(nodes)
                .build();
    }

    public BusinessUnitResponse update(String organizationId, String id, BusinessUnitUpdateRequest request, ActorResolver.Actor actor) {
        accessControlService.requireBusinessUnitManage(organizationId, id, actor);
        BusinessUnit businessUnit = find(organizationId, id);
        Map<String, Object> before = businessUnitFields(businessUnit);
        if (StringUtils.hasText(request.getCode())) {
            String code = SlugNormalizer.normalizeCode(request.getCode());
            if (!code.equals(businessUnit.getCode()) && businessUnitRepository.existsByOrganizationIdAndCode(organizationId, code)) {
                throw new DuplicateResourceException("Business unit code already exists for this organization: " + code);
            }
            businessUnit.setCode(code);
        }
        if (StringUtils.hasText(request.getName())) businessUnit.setName(request.getName().trim());
        if (request.getOwnerName() != null) businessUnit.setOwnerName(SlugNormalizer.trimToNull(request.getOwnerName()));
        if (request.getOwnerEmail() != null) businessUnit.setOwnerEmail(SlugNormalizer.trimToNull(request.getOwnerEmail()));
        if (request.getCostCenter() != null) businessUnit.setCostCenter(SlugNormalizer.trimToNull(request.getCostCenter()));
        if (request.getDescription() != null) businessUnit.setDescription(SlugNormalizer.trimToNull(request.getDescription()));
        if (request.getStatus() != null) businessUnit.setStatus(request.getStatus());
        businessUnit.setUpdatedBy(actor.name());
        businessUnit.setUpdatedByEmail(actor.email());
        BusinessUnit saved = businessUnitRepository.save(businessUnit);
        var changes = FieldChangeDetector.diff(before, businessUnitFields(saved));
        auditService.record(organizationId, ResourceType.BUSINESS_UNIT, id, statusChanged(before, saved.getStatus()) ? AuditAction.STATUS_CHANGE : AuditAction.UPDATE, actor, changes, before, saved);
        return toResponse(saved);
    }

    public void delete(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireBusinessUnitManage(organizationId, id, actor);
        BusinessUnit businessUnit = find(organizationId, id);
        Map<String, Object> before = businessUnitFields(businessUnit);
        businessUnit.setStatus(BusinessUnitStatus.DELETED);
        businessUnit.setDeletedAt(Instant.now());
        businessUnit.setDeletedBy(actor.name());
        businessUnit.setDeletedByEmail(actor.email());
        businessUnit.setUpdatedBy(actor.name());
        businessUnit.setUpdatedByEmail(actor.email());
        BusinessUnit saved = businessUnitRepository.save(businessUnit);
        auditService.record(organizationId, ResourceType.BUSINESS_UNIT, id, AuditAction.DELETE, actor, FieldChangeDetector.diff(before, businessUnitFields(saved)), before, saved);
    }

    public List<AuditLogResponse> history(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireBusinessUnitView(organizationId, id, actor);
        find(organizationId, id);
        return auditService.history(organizationId, ResourceType.BUSINESS_UNIT, id);
    }

    public BusinessUnit find(String organizationId, String id) {
        return businessUnitRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Business unit not found: " + id));
    }

    public BusinessUnitResponse toResponse(BusinessUnit businessUnit) {
        return BusinessUnitResponse.builder()
                .id(businessUnit.getId())
                .organizationId(businessUnit.getOrganizationId())
                .name(businessUnit.getName())
                .code(businessUnit.getCode())
                .ownerName(businessUnit.getOwnerName())
                .ownerEmail(businessUnit.getOwnerEmail())
                .costCenter(businessUnit.getCostCenter())
                .description(businessUnit.getDescription())
                .status(businessUnit.getStatus())
                .projectCount(projectRepository.countByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNull(businessUnit.getOrganizationId(), businessUnit.getId()))
                .applicationCount(applicationRepository.countByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNull(businessUnit.getOrganizationId(), businessUnit.getId()))
                .createdBy(businessUnit.getCreatedBy())
                .createdByEmail(businessUnit.getCreatedByEmail())
                .updatedBy(businessUnit.getUpdatedBy())
                .updatedByEmail(businessUnit.getUpdatedByEmail())
                .createdAt(businessUnit.getCreatedAt())
                .updatedAt(businessUnit.getUpdatedAt())
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

    private boolean matches(String search, String... values) {
        if (!StringUtils.hasText(search)) return true;
        String needle = search.trim().toLowerCase();
        for (String value : values) {
            if (value != null && value.toLowerCase().contains(needle)) return true;
        }
        return false;
    }

    private boolean statusChanged(Map<String, Object> before, BusinessUnitStatus status) {
        return !String.valueOf(before.get("status")).equals(String.valueOf(status));
    }

    private Map<String, Object> businessUnitFields(BusinessUnit businessUnit) {
        return Map.of(
                "name", nullToEmpty(businessUnit.getName()),
                "code", nullToEmpty(businessUnit.getCode()),
                "ownerName", nullToEmpty(businessUnit.getOwnerName()),
                "ownerEmail", nullToEmpty(businessUnit.getOwnerEmail()),
                "costCenter", nullToEmpty(businessUnit.getCostCenter()),
                "description", nullToEmpty(businessUnit.getDescription()),
                "status", businessUnit.getStatus()
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

