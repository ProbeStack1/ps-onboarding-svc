package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.application.ApplicationConsumersRequest;
import io.probestack.onboarding.dto.application.ApplicationCreateRequest;
import io.probestack.onboarding.dto.application.ApplicationResponse;
import io.probestack.onboarding.dto.application.ApplicationUpdateRequest;
import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.model.ApplicationStatus;
import io.probestack.onboarding.service.ApplicationService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding/applications")
public class ApplicationController extends ResponseSupport {
    private final ApplicationService applicationService;
    private final ActorResolver actorResolver;

    public ApplicationController(ApplicationService applicationService, ActorResolver actorResolver) {
        this.applicationService = applicationService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> create(@Valid @RequestBody ApplicationCreateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return created("Application created successfully", applicationService.create(organizationId, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> list(@RequestParam(required = false) String businessUnitId,
                                                                       @RequestParam(required = false) String projectId,
                                                                       @RequestParam(required = false) String search,
                                                                       @RequestParam(required = false) ApplicationStatus status,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size,
                                                                       HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return page("Applications fetched successfully", applicationService.list(organizationId, businessUnitId, projectId, search, status, page, size, actorResolver.requireActor(null, request)), page, size, request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> get(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Application fetched successfully", applicationService.get(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> update(@PathVariable String id, @Valid @RequestBody ApplicationUpdateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Application updated successfully", applicationService.update(organizationId, id, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id, @RequestBody(required = false) ApplicationUpdateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        applicationService.delete(organizationId, id, actorResolver.requireActor(body == null ? null : body.getActor(), request));
        return noData("Application deleted successfully", request);
    }

    @PutMapping("/{id}/consumers")
    public ResponseEntity<ApiResponse<ApplicationResponse>> replaceConsumers(@PathVariable String id, @Valid @RequestBody ApplicationConsumersRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Application consumers updated successfully", applicationService.replaceConsumers(organizationId, id, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> history(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Application history fetched successfully", applicationService.history(organizationId, id, actorResolver.requireActor(null, request)), request);
    }
}

