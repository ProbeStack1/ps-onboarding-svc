package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.dto.developer.DeveloperCreateRequest;
import io.probestack.onboarding.dto.developer.DeveloperResponse;
import io.probestack.onboarding.dto.developer.DeveloperUpdateRequest;
import io.probestack.onboarding.model.DeveloperAccountStatus;
import io.probestack.onboarding.service.DeveloperService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding/developers")
public class DeveloperController extends ResponseSupport {
    private final DeveloperService developerService;
    private final ActorResolver actorResolver;

    public DeveloperController(DeveloperService developerService, ActorResolver actorResolver) {
        this.developerService = developerService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeveloperResponse>> create(@Valid @RequestBody DeveloperCreateRequest body,
                                                                  HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return created("Developer created successfully",
                developerService.create(organizationId, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeveloperResponse>>> list(@RequestParam(required = false) String search,
                                                                     @RequestParam(required = false) DeveloperAccountStatus status,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size,
                                                                     HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return page("Developers fetched successfully",
                developerService.list(organizationId, search, status, page, size, actorResolver.requireActor(null, request)),
                page, size, request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeveloperResponse>> get(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Developer fetched successfully", developerService.get(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<DeveloperResponse>> update(@PathVariable String id,
                                                                  @Valid @RequestBody DeveloperUpdateRequest body,
                                                                  HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Developer updated successfully",
                developerService.update(organizationId, id, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id,
                                                     @RequestBody(required = false) DeveloperUpdateRequest body,
                                                     HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        developerService.delete(organizationId, id, actorResolver.requireActor(body == null ? null : body.getActor(), request));
        return noData("Developer deleted successfully", request);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> history(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Developer history fetched successfully",
                developerService.history(organizationId, id, actorResolver.requireActor(null, request)), request);
    }
}
