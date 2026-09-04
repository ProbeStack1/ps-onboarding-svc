package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.adminaccess.AdminResourceAccessResponse;
import io.probestack.onboarding.dto.adminaccess.AdminResourceType;
import io.probestack.onboarding.dto.adminaccess.AdminUserAccessResponse;
import io.probestack.onboarding.dto.adminaccess.UserAccessBootstrapResponse;
import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.service.AdminAccessCatalogService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding/admin/access-catalog")
public class AdminAccessCatalogController extends ResponseSupport {
    private final AdminAccessCatalogService catalogService;
    private final ActorResolver actorResolver;

    public AdminAccessCatalogController(
            AdminAccessCatalogService catalogService,
            ActorResolver actorResolver) {
        this.catalogService = catalogService;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserAccessResponse>>> users(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return page("Admin user access catalog fetched successfully",
                catalogService.listUsers(
                        organizationId,
                        search,
                        status,
                        page,
                        size,
                        request.getHeader(HttpHeaders.AUTHORIZATION),
                        actorResolver.requireActor(null, request)),
                page,
                size,
                request);
    }

    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<List<AdminResourceAccessResponse>>> resources(
            @RequestParam(required = false) AdminResourceType resourceType,
            @RequestParam(defaultValue = "false") boolean includeInactiveUsers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return page("Admin resource access catalog fetched successfully",
                catalogService.listResources(
                        organizationId,
                        resourceType,
                        includeInactiveUsers,
                        page,
                        size,
                        request.getHeader(HttpHeaders.AUTHORIZATION),
                        actorResolver.requireActor(null, request)),
                page,
                size,
                request);
    }

    @GetMapping("/users/{principalId}/bootstrap")
    public ResponseEntity<ApiResponse<UserAccessBootstrapResponse>> bootstrap(
            @PathVariable String principalId,
            HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("User login access bootstrap fetched successfully",
                catalogService.bootstrap(
                        organizationId,
                        principalId,
                        request.getHeader(HttpHeaders.AUTHORIZATION),
                        actorResolver.requireActor(null, request)),
                request);
    }

    @GetMapping("/users/{principalId}")
    public ResponseEntity<ApiResponse<AdminUserAccessResponse>> user(
            @PathVariable String principalId,
            HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Admin user access fetched successfully",
                catalogService.getUser(
                        organizationId,
                        principalId,
                        request.getHeader(HttpHeaders.AUTHORIZATION),
                        actorResolver.requireActor(null, request)),
                request);
    }

    @GetMapping("/resources/{resourceType}/{resourceId}")
    public ResponseEntity<ApiResponse<AdminResourceAccessResponse>> resource(
            @PathVariable AdminResourceType resourceType,
            @PathVariable String resourceId,
            @RequestParam(defaultValue = "false") boolean includeInactiveUsers,
            HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Admin resource access fetched successfully",
                catalogService.getResource(
                        organizationId,
                        resourceType,
                        resourceId,
                        includeInactiveUsers,
                        request.getHeader(HttpHeaders.AUTHORIZATION),
                        actorResolver.requireActor(null, request)),
                request);
    }
}
