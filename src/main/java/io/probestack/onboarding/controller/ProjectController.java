package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.application.ApplicationSummaryResponse;
import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.dto.project.ProjectCreateRequest;
import io.probestack.onboarding.dto.project.ProjectResponse;
import io.probestack.onboarding.dto.project.ProjectUpdateRequest;
import io.probestack.onboarding.model.ProjectStatus;
import io.probestack.onboarding.service.ProjectService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding/projects")
public class ProjectController extends ResponseSupport {
    private final ProjectService projectService;
    private final ActorResolver actorResolver;

    public ProjectController(ProjectService projectService, ActorResolver actorResolver) {
        this.projectService = projectService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> create(@Valid @RequestBody ProjectCreateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return created("Project created successfully", projectService.create(organizationId, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> list(@RequestParam(required = false) String businessUnitId,
                                                                   @RequestParam(required = false) String search,
                                                                   @RequestParam(required = false) ProjectStatus status,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size,
                                                                   HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return page("Projects fetched successfully", projectService.list(organizationId, businessUnitId, search, status, page, size, actorResolver.requireActor(null, request)), page, size, request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> get(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Project fetched successfully", projectService.get(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(@PathVariable String id, @Valid @RequestBody ProjectUpdateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Project updated successfully", projectService.update(organizationId, id, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id, @RequestBody(required = false) ProjectUpdateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        projectService.delete(organizationId, id, actorResolver.requireActor(body == null ? null : body.getActor(), request));
        return noData("Project deleted successfully", request);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> history(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Project history fetched successfully", projectService.history(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/{id}/applications")
    public ResponseEntity<ApiResponse<List<ApplicationSummaryResponse>>> applications(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Project applications fetched successfully", projectService.applications(organizationId, id, actorResolver.requireActor(null, request)), request);
    }
}

