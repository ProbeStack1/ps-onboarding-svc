package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.dto.member.MemberRoleAssignmentResponse;
import io.probestack.onboarding.dto.member.RoleAssignmentCreateRequest;
import io.probestack.onboarding.dto.member.RoleAssignmentUpdateRequest;
import io.probestack.onboarding.service.PrincipalRoleAssignmentService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding/role-assignments")
public class PrincipalRoleAssignmentController extends ResponseSupport {
    private final PrincipalRoleAssignmentService assignmentService;
    private final ActorResolver actorResolver;

    public PrincipalRoleAssignmentController(
            PrincipalRoleAssignmentService assignmentService,
            ActorResolver actorResolver) {
        this.assignmentService = assignmentService;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberRoleAssignmentResponse>>> list(HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Role assignments fetched successfully",
                assignmentService.list(organizationId, actorResolver.requireActor(null, request)), request);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberRoleAssignmentResponse>> create(
            @Valid @RequestBody RoleAssignmentCreateRequest body,
            HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return created("Role assignment created successfully",
                assignmentService.create(
                        organizationId,
                        body,
                        request.getHeader(HttpHeaders.AUTHORIZATION),
                        actorResolver.requireActor(null, request)),
                request);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberRoleAssignmentResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody RoleAssignmentUpdateRequest body,
            HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Role assignment updated successfully",
                assignmentService.update(organizationId, id, body, actorResolver.requireActor(null, request)), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        assignmentService.revoke(organizationId, id, actorResolver.requireActor(null, request));
        return noData("Role assignment revoked successfully", request);
    }
}
