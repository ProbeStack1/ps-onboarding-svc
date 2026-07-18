package io.probestack.onboarding.service;

import io.probestack.onboarding.dto.businessunit.BusinessUnitTreeResponse;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.dto.common.SelectorOption;
import io.probestack.onboarding.dto.dashboard.DashboardSummaryResponse;
import io.probestack.onboarding.dto.dashboard.HierarchyResponse;
import io.probestack.onboarding.model.BusinessUnitStatus;
import io.probestack.onboarding.model.ConsumerStatus;
import io.probestack.onboarding.util.ActorResolver;
import io.probestack.onboarding.model.ProjectStatus;
import io.probestack.onboarding.repository.ApplicationRepository;
import io.probestack.onboarding.repository.BusinessUnitRepository;
import io.probestack.onboarding.repository.ConsumerRepository;
import io.probestack.onboarding.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DashboardService {
    private final BusinessUnitRepository businessUnitRepository;
    private final ProjectRepository projectRepository;
    private final ApplicationRepository applicationRepository;
    private final ConsumerRepository consumerRepository;
    private final BusinessUnitService businessUnitService;
    private final PagingService pagingService;
    private final AccessControlService accessControlService;

    public DashboardService(BusinessUnitRepository businessUnitRepository, ProjectRepository projectRepository,
                            ApplicationRepository applicationRepository, ConsumerRepository consumerRepository,
                            BusinessUnitService businessUnitService, PagingService pagingService, AccessControlService accessControlService) {
        this.businessUnitRepository = businessUnitRepository;
        this.projectRepository = projectRepository;
        this.applicationRepository = applicationRepository;
        this.consumerRepository = consumerRepository;
        this.businessUnitService = businessUnitService;
        this.pagingService = pagingService;
        this.accessControlService = accessControlService;
    }

    public DashboardSummaryResponse summary(String organizationId, ActorResolver.Actor actor) {
        var units = accessControlService.filterBusinessUnits(organizationId, businessUnitRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId), actor);
        var projects = accessControlService.filterProjects(organizationId, projectRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId), actor);
        var apps = accessControlService.filterApplications(organizationId, applicationRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId), actor);
        var consumers = accessControlService.filterConsumers(organizationId, consumerRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId), actor);
        return DashboardSummaryResponse.builder()
                .businessUnits(units.size())
                .projects(projects.size())
                .applications(apps.size())
                .consumers(consumers.size())
                .build();
    }

    public PagedResult<BusinessUnitTreeResponse> hierarchy(String organizationId, int page, int size, ActorResolver.Actor actor) {
        List<BusinessUnitTreeResponse> trees = accessControlService.filterBusinessUnits(organizationId, businessUnitRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId), actor)
                .stream().map(bu -> businessUnitService.tree(organizationId, bu.getId(), actor)).toList();
        return pagingService.page(trees, page, size);
    }

    public List<SelectorOption> businessUnitOptions(String organizationId, BusinessUnitStatus status, ActorResolver.Actor actor) {
        var units = status == null
                ? businessUnitRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId)
                : businessUnitRepository.findByOrganizationIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, status);
        return accessControlService.filterBusinessUnits(organizationId, units, actor).stream().map(item -> SelectorOption.builder()
                .id(item.getId())
                .label(item.getName())
                .code(item.getCode())
                .status(item.getStatus().name())
                .build()).toList();
    }

    public List<SelectorOption> projectOptions(String organizationId, String businessUnitId, ProjectStatus status, ActorResolver.Actor actor) {
        var projects = StringUtils.hasText(businessUnitId)
                ? (status == null
                    ? projectRepository.findByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, businessUnitId)
                    : projectRepository.findByOrganizationIdAndBusinessUnitIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, businessUnitId, status))
                : projectRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId).stream()
                    .filter(project -> status == null || project.getStatus() == status).toList();
        return accessControlService.filterProjects(organizationId, projects, actor).stream().map(item -> SelectorOption.builder()
                .id(item.getId())
                .label(item.getName())
                .code(item.getCode())
                .status(item.getStatus().name())
                .build()).toList();
    }

    public PagedResult<SelectorOption> consumerOptions(String organizationId, String search, int page, int size, ActorResolver.Actor actor) {
        List<SelectorOption> options = accessControlService.filterConsumers(organizationId, consumerRepository.findByOrganizationIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, ConsumerStatus.ACTIVE), actor)
                .stream()
                .filter(item -> !StringUtils.hasText(search) || item.getName().toLowerCase().contains(search.trim().toLowerCase()))
                .map(item -> SelectorOption.builder()
                        .id(item.getId())
                        .label(item.getName())
                        .code(item.getPocEmail())
                        .status(item.getStatus().name())
                        .build())
                .toList();
        return pagingService.page(options, page, size);
    }
}

