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
import io.probestack.onboarding.model.BusinessUnitQuota;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        String parentBusinessUnitId = SlugNormalizer.trimToNull(request.getParentBusinessUnitId());
        validateParent(organizationId, null, parentBusinessUnitId);
        BusinessUnit businessUnit = BusinessUnit.builder()
                .organizationId(organizationId)
                .name(request.getName().trim())
                .code(code)
                .displayName(SlugNormalizer.trimToNull(request.getDisplayName()))
                .parentBusinessUnitId(parentBusinessUnitId)
                .division(SlugNormalizer.trimToNull(request.getDivision()))
                .department(SlugNormalizer.trimToNull(request.getDepartment()))
                .lineOfBusiness(SlugNormalizer.trimToNull(request.getLineOfBusiness()))
                .ownerName(SlugNormalizer.trimToNull(request.getOwnerName()))
                .ownerEmail(SlugNormalizer.trimToNull(request.getOwnerEmail()))
                .businessExecutiveId(SlugNormalizer.trimToNull(request.getBusinessExecutiveId()))
                .businessOwnerId(SlugNormalizer.trimToNull(request.getBusinessOwnerId()))
                .productOwnerId(SlugNormalizer.trimToNull(request.getProductOwnerId()))
                .technicalOwnerId(SlugNormalizer.trimToNull(request.getTechnicalOwnerId()))
                .enterpriseArchitectId(SlugNormalizer.trimToNull(request.getEnterpriseArchitectId()))
                .platformOwnerId(SlugNormalizer.trimToNull(request.getPlatformOwnerId()))
                .securityOwnerId(SlugNormalizer.trimToNull(request.getSecurityOwnerId()))
                .complianceOfficerId(SlugNormalizer.trimToNull(request.getComplianceOfficerId()))
                .supportTeam(SlugNormalizer.trimToNull(request.getSupportTeam()))
                .operationsTeam(SlugNormalizer.trimToNull(request.getOperationsTeam()))
                .costCenter(SlugNormalizer.trimToNull(request.getCostCenter()))
                .budget(request.getBudget())
                .chargebackModel(SlugNormalizer.trimToNull(request.getChargebackModel()))
                .billingAccount(SlugNormalizer.trimToNull(request.getBillingAccount()))
                .monthlyBudget(request.getMonthlyBudget())
                .annualBudget(request.getAnnualBudget())
                .aiBudget(request.getAiBudget())
                .apiBudget(request.getApiBudget())
                .cloudProvider(SlugNormalizer.trimToNull(request.getCloudProvider()))
                .region(SlugNormalizer.trimToNull(request.getRegion()))
                .kubernetesCluster(SlugNormalizer.trimToNull(request.getKubernetesCluster()))
                .namespace(SlugNormalizer.trimToNull(request.getNamespace()))
                .apiGateway(SlugNormalizer.trimToNull(request.getApiGateway()))
                .aiGateway(SlugNormalizer.trimToNull(request.getAiGateway()))
                .loggingPlatform(SlugNormalizer.trimToNull(request.getLoggingPlatform()))
                .monitoringPlatform(SlugNormalizer.trimToNull(request.getMonitoringPlatform()))
                .secretManager(SlugNormalizer.trimToNull(request.getSecretManager()))
                .approvalWorkflow(SlugNormalizer.trimToNull(request.getApprovalWorkflow()))
                .riskClassification(SlugNormalizer.trimToNull(request.getRiskClassification()))
                .businessCriticality(SlugNormalizer.trimToNull(request.getBusinessCriticality()))
                .dataClassification(SlugNormalizer.trimToNull(request.getDataClassification()))
                .regulatoryStandards(normalizeStrings(request.getRegulatoryStandards()))
                .retentionPolicy(SlugNormalizer.trimToNull(request.getRetentionPolicy()))
                .backupPolicy(SlugNormalizer.trimToNull(request.getBackupPolicy()))
                .drEnabled(request.isDrEnabled())
                .slaTier(SlugNormalizer.trimToNull(request.getSlaTier()))
                .quotas(normalizeQuotas(request.getQuotas(), List.of()))
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
        if (request.getDisplayName() != null) businessUnit.setDisplayName(SlugNormalizer.trimToNull(request.getDisplayName()));
        if (request.getParentBusinessUnitId() != null) {
            String parentId = SlugNormalizer.trimToNull(request.getParentBusinessUnitId());
            validateParent(organizationId, id, parentId);
            businessUnit.setParentBusinessUnitId(parentId);
        }
        if (request.getDivision() != null) businessUnit.setDivision(SlugNormalizer.trimToNull(request.getDivision()));
        if (request.getDepartment() != null) businessUnit.setDepartment(SlugNormalizer.trimToNull(request.getDepartment()));
        if (request.getLineOfBusiness() != null) businessUnit.setLineOfBusiness(SlugNormalizer.trimToNull(request.getLineOfBusiness()));
        if (request.getOwnerName() != null) businessUnit.setOwnerName(SlugNormalizer.trimToNull(request.getOwnerName()));
        if (request.getOwnerEmail() != null) businessUnit.setOwnerEmail(SlugNormalizer.trimToNull(request.getOwnerEmail()));
        if (request.getBusinessExecutiveId() != null) businessUnit.setBusinessExecutiveId(SlugNormalizer.trimToNull(request.getBusinessExecutiveId()));
        if (request.getBusinessOwnerId() != null) businessUnit.setBusinessOwnerId(SlugNormalizer.trimToNull(request.getBusinessOwnerId()));
        if (request.getProductOwnerId() != null) businessUnit.setProductOwnerId(SlugNormalizer.trimToNull(request.getProductOwnerId()));
        if (request.getTechnicalOwnerId() != null) businessUnit.setTechnicalOwnerId(SlugNormalizer.trimToNull(request.getTechnicalOwnerId()));
        if (request.getEnterpriseArchitectId() != null) businessUnit.setEnterpriseArchitectId(SlugNormalizer.trimToNull(request.getEnterpriseArchitectId()));
        if (request.getPlatformOwnerId() != null) businessUnit.setPlatformOwnerId(SlugNormalizer.trimToNull(request.getPlatformOwnerId()));
        if (request.getSecurityOwnerId() != null) businessUnit.setSecurityOwnerId(SlugNormalizer.trimToNull(request.getSecurityOwnerId()));
        if (request.getComplianceOfficerId() != null) businessUnit.setComplianceOfficerId(SlugNormalizer.trimToNull(request.getComplianceOfficerId()));
        if (request.getSupportTeam() != null) businessUnit.setSupportTeam(SlugNormalizer.trimToNull(request.getSupportTeam()));
        if (request.getOperationsTeam() != null) businessUnit.setOperationsTeam(SlugNormalizer.trimToNull(request.getOperationsTeam()));
        if (request.getCostCenter() != null) businessUnit.setCostCenter(SlugNormalizer.trimToNull(request.getCostCenter()));
        if (request.getBudget() != null) businessUnit.setBudget(request.getBudget());
        if (request.getChargebackModel() != null) businessUnit.setChargebackModel(SlugNormalizer.trimToNull(request.getChargebackModel()));
        if (request.getBillingAccount() != null) businessUnit.setBillingAccount(SlugNormalizer.trimToNull(request.getBillingAccount()));
        if (request.getMonthlyBudget() != null) businessUnit.setMonthlyBudget(request.getMonthlyBudget());
        if (request.getAnnualBudget() != null) businessUnit.setAnnualBudget(request.getAnnualBudget());
        if (request.getAiBudget() != null) businessUnit.setAiBudget(request.getAiBudget());
        if (request.getApiBudget() != null) businessUnit.setApiBudget(request.getApiBudget());
        if (request.getCloudProvider() != null) businessUnit.setCloudProvider(SlugNormalizer.trimToNull(request.getCloudProvider()));
        if (request.getRegion() != null) businessUnit.setRegion(SlugNormalizer.trimToNull(request.getRegion()));
        if (request.getKubernetesCluster() != null) businessUnit.setKubernetesCluster(SlugNormalizer.trimToNull(request.getKubernetesCluster()));
        if (request.getNamespace() != null) businessUnit.setNamespace(SlugNormalizer.trimToNull(request.getNamespace()));
        if (request.getApiGateway() != null) businessUnit.setApiGateway(SlugNormalizer.trimToNull(request.getApiGateway()));
        if (request.getAiGateway() != null) businessUnit.setAiGateway(SlugNormalizer.trimToNull(request.getAiGateway()));
        if (request.getLoggingPlatform() != null) businessUnit.setLoggingPlatform(SlugNormalizer.trimToNull(request.getLoggingPlatform()));
        if (request.getMonitoringPlatform() != null) businessUnit.setMonitoringPlatform(SlugNormalizer.trimToNull(request.getMonitoringPlatform()));
        if (request.getSecretManager() != null) businessUnit.setSecretManager(SlugNormalizer.trimToNull(request.getSecretManager()));
        if (request.getApprovalWorkflow() != null) businessUnit.setApprovalWorkflow(SlugNormalizer.trimToNull(request.getApprovalWorkflow()));
        if (request.getRiskClassification() != null) businessUnit.setRiskClassification(SlugNormalizer.trimToNull(request.getRiskClassification()));
        if (request.getBusinessCriticality() != null) businessUnit.setBusinessCriticality(SlugNormalizer.trimToNull(request.getBusinessCriticality()));
        if (request.getDataClassification() != null) businessUnit.setDataClassification(SlugNormalizer.trimToNull(request.getDataClassification()));
        if (request.getRegulatoryStandards() != null) businessUnit.setRegulatoryStandards(normalizeStrings(request.getRegulatoryStandards()));
        if (request.getRetentionPolicy() != null) businessUnit.setRetentionPolicy(SlugNormalizer.trimToNull(request.getRetentionPolicy()));
        if (request.getBackupPolicy() != null) businessUnit.setBackupPolicy(SlugNormalizer.trimToNull(request.getBackupPolicy()));
        if (request.getDrEnabled() != null) businessUnit.setDrEnabled(request.getDrEnabled());
        if (request.getSlaTier() != null) businessUnit.setSlaTier(SlugNormalizer.trimToNull(request.getSlaTier()));
        if (request.getQuotas() != null) businessUnit.setQuotas(normalizeQuotas(request.getQuotas(), businessUnit.getQuotas()));
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
                .displayName(businessUnit.getDisplayName())
                .parentBusinessUnitId(businessUnit.getParentBusinessUnitId())
                .division(businessUnit.getDivision())
                .department(businessUnit.getDepartment())
                .lineOfBusiness(businessUnit.getLineOfBusiness())
                .ownerName(businessUnit.getOwnerName())
                .ownerEmail(businessUnit.getOwnerEmail())
                .businessExecutiveId(businessUnit.getBusinessExecutiveId())
                .businessOwnerId(businessUnit.getBusinessOwnerId())
                .productOwnerId(businessUnit.getProductOwnerId())
                .technicalOwnerId(businessUnit.getTechnicalOwnerId())
                .enterpriseArchitectId(businessUnit.getEnterpriseArchitectId())
                .platformOwnerId(businessUnit.getPlatformOwnerId())
                .securityOwnerId(businessUnit.getSecurityOwnerId())
                .complianceOfficerId(businessUnit.getComplianceOfficerId())
                .supportTeam(businessUnit.getSupportTeam())
                .operationsTeam(businessUnit.getOperationsTeam())
                .costCenter(businessUnit.getCostCenter())
                .budget(businessUnit.getBudget())
                .chargebackModel(businessUnit.getChargebackModel())
                .billingAccount(businessUnit.getBillingAccount())
                .monthlyBudget(businessUnit.getMonthlyBudget())
                .annualBudget(businessUnit.getAnnualBudget())
                .aiBudget(businessUnit.getAiBudget())
                .apiBudget(businessUnit.getApiBudget())
                .cloudProvider(businessUnit.getCloudProvider())
                .region(businessUnit.getRegion())
                .kubernetesCluster(businessUnit.getKubernetesCluster())
                .namespace(businessUnit.getNamespace())
                .apiGateway(businessUnit.getApiGateway())
                .aiGateway(businessUnit.getAiGateway())
                .loggingPlatform(businessUnit.getLoggingPlatform())
                .monitoringPlatform(businessUnit.getMonitoringPlatform())
                .secretManager(businessUnit.getSecretManager())
                .approvalWorkflow(businessUnit.getApprovalWorkflow())
                .riskClassification(businessUnit.getRiskClassification())
                .businessCriticality(businessUnit.getBusinessCriticality())
                .dataClassification(businessUnit.getDataClassification())
                .regulatoryStandards(safeList(businessUnit.getRegulatoryStandards()))
                .retentionPolicy(businessUnit.getRetentionPolicy())
                .backupPolicy(businessUnit.getBackupPolicy())
                .drEnabled(businessUnit.isDrEnabled())
                .slaTier(businessUnit.getSlaTier())
                .quotas(safeList(businessUnit.getQuotas()))
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
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", value(businessUnit.getName()));
        fields.put("code", value(businessUnit.getCode()));
        fields.put("displayName", value(businessUnit.getDisplayName()));
        fields.put("parentBusinessUnitId", value(businessUnit.getParentBusinessUnitId()));
        fields.put("division", value(businessUnit.getDivision()));
        fields.put("department", value(businessUnit.getDepartment()));
        fields.put("lineOfBusiness", value(businessUnit.getLineOfBusiness()));
        fields.put("ownerName", value(businessUnit.getOwnerName()));
        fields.put("ownerEmail", value(businessUnit.getOwnerEmail()));
        fields.put("businessExecutiveId", value(businessUnit.getBusinessExecutiveId()));
        fields.put("businessOwnerId", value(businessUnit.getBusinessOwnerId()));
        fields.put("productOwnerId", value(businessUnit.getProductOwnerId()));
        fields.put("technicalOwnerId", value(businessUnit.getTechnicalOwnerId()));
        fields.put("enterpriseArchitectId", value(businessUnit.getEnterpriseArchitectId()));
        fields.put("platformOwnerId", value(businessUnit.getPlatformOwnerId()));
        fields.put("securityOwnerId", value(businessUnit.getSecurityOwnerId()));
        fields.put("complianceOfficerId", value(businessUnit.getComplianceOfficerId()));
        fields.put("supportTeam", value(businessUnit.getSupportTeam()));
        fields.put("operationsTeam", value(businessUnit.getOperationsTeam()));
        fields.put("costCenter", value(businessUnit.getCostCenter()));
        fields.put("budget", value(businessUnit.getBudget()));
        fields.put("chargebackModel", value(businessUnit.getChargebackModel()));
        fields.put("billingAccount", value(businessUnit.getBillingAccount()));
        fields.put("monthlyBudget", value(businessUnit.getMonthlyBudget()));
        fields.put("annualBudget", value(businessUnit.getAnnualBudget()));
        fields.put("aiBudget", value(businessUnit.getAiBudget()));
        fields.put("apiBudget", value(businessUnit.getApiBudget()));
        fields.put("cloudProvider", value(businessUnit.getCloudProvider()));
        fields.put("region", value(businessUnit.getRegion()));
        fields.put("kubernetesCluster", value(businessUnit.getKubernetesCluster()));
        fields.put("namespace", value(businessUnit.getNamespace()));
        fields.put("apiGateway", value(businessUnit.getApiGateway()));
        fields.put("aiGateway", value(businessUnit.getAiGateway()));
        fields.put("loggingPlatform", value(businessUnit.getLoggingPlatform()));
        fields.put("monitoringPlatform", value(businessUnit.getMonitoringPlatform()));
        fields.put("secretManager", value(businessUnit.getSecretManager()));
        fields.put("approvalWorkflow", value(businessUnit.getApprovalWorkflow()));
        fields.put("riskClassification", value(businessUnit.getRiskClassification()));
        fields.put("businessCriticality", value(businessUnit.getBusinessCriticality()));
        fields.put("dataClassification", value(businessUnit.getDataClassification()));
        fields.put("regulatoryStandards", safeList(businessUnit.getRegulatoryStandards()));
        fields.put("retentionPolicy", value(businessUnit.getRetentionPolicy()));
        fields.put("backupPolicy", value(businessUnit.getBackupPolicy()));
        fields.put("drEnabled", businessUnit.isDrEnabled());
        fields.put("slaTier", value(businessUnit.getSlaTier()));
        fields.put("quotas", safeList(businessUnit.getQuotas()));
        fields.put("description", value(businessUnit.getDescription()));
        fields.put("status", businessUnit.getStatus());
        return fields;
    }

    private void validateParent(String organizationId, String businessUnitId, String parentId) {
        if (parentId == null) return;
        if (parentId.equals(businessUnitId)) throw new IllegalArgumentException("A business unit cannot be its own parent");
        BusinessUnit parent = find(organizationId, parentId);
        Set<String> visited = new HashSet<>();
        while (parent.getParentBusinessUnitId() != null && visited.add(parent.getId())) {
            if (parent.getParentBusinessUnitId().equals(businessUnitId)) {
                throw new IllegalArgumentException("Parent business unit selection would create a cycle");
            }
            parent = find(organizationId, parent.getParentBusinessUnitId());
        }
    }

    private List<BusinessUnitQuota> normalizeQuotas(List<BusinessUnitQuota> requested, List<BusinessUnitQuota> existing) {
        if (requested == null) return new ArrayList<>();
        Map<String, BigDecimal> usageByType = new HashMap<>();
        safeList(existing).forEach(quota -> usageByType.put(quota.getQuotaType(), quota.getQuotaUsed() == null ? BigDecimal.ZERO : quota.getQuotaUsed()));
        Set<String> types = new HashSet<>();
        List<BusinessUnitQuota> normalized = new ArrayList<>();
        for (BusinessUnitQuota quota : requested) {
            String type = SlugNormalizer.trimToNull(quota.getQuotaType());
            if (type == null || !types.add(type)) throw new IllegalArgumentException("Each quota type may be configured only once");
            normalized.add(BusinessUnitQuota.builder()
                    .quotaType(type)
                    .quotaLimit(quota.getQuotaLimit())
                    .quotaUsed(usageByType.getOrDefault(type, BigDecimal.ZERO))
                    .build());
        }
        return normalized;
    }

    private List<String> normalizeStrings(List<String> values) {
        if (values == null) return new ArrayList<>();
        return values.stream().map(SlugNormalizer::trimToNull).filter(java.util.Objects::nonNull).distinct().toList();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }
}

