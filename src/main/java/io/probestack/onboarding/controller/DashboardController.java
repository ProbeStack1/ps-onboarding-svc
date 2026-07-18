package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.businessunit.BusinessUnitTreeResponse;
import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.dto.common.SelectorOption;
import io.probestack.onboarding.dto.dashboard.DashboardSummaryResponse;
import io.probestack.onboarding.model.BusinessUnitStatus;
import io.probestack.onboarding.model.ProjectStatus;
import io.probestack.onboarding.service.DashboardService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding")
public class DashboardController extends ResponseSupport {
    private final DashboardService dashboardService;
    private final ActorResolver actorResolver;

    public DashboardController(DashboardService dashboardService, ActorResolver actorResolver) {
        this.dashboardService = dashboardService;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Onboarding summary fetched successfully", dashboardService.summary(organizationId, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/dashboard/hierarchy")
    public ResponseEntity<ApiResponse<List<BusinessUnitTreeResponse>>> hierarchy(@RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "20") int size,
                                                                                 HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return page("Onboarding hierarchy fetched successfully", dashboardService.hierarchy(organizationId, page, size, actorResolver.requireActor(null, request)), page, size, request);
    }

    @GetMapping("/selectors/business-units")
    public ResponseEntity<ApiResponse<List<SelectorOption>>> businessUnits(@RequestParam(required = false) BusinessUnitStatus status, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Business unit options fetched successfully", dashboardService.businessUnitOptions(organizationId, status, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/selectors/projects")
    public ResponseEntity<ApiResponse<List<SelectorOption>>> projects(@RequestParam(required = false) String businessUnitId,
                                                                      @RequestParam(required = false) ProjectStatus status,
                                                                      HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Project options fetched successfully", dashboardService.projectOptions(organizationId, businessUnitId, status, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/selectors/consumers")
    public ResponseEntity<ApiResponse<List<SelectorOption>>> consumers(@RequestParam(required = false) String search,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size,
                                                                       HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return page("Consumer options fetched successfully", dashboardService.consumerOptions(organizationId, search, page, size, actorResolver.requireActor(null, request)), page, size, request);
    }
}

