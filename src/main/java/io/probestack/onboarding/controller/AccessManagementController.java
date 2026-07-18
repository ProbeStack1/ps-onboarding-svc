package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.access.*;
import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.service.AccessManagementService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding/access")
public class AccessManagementController extends ResponseSupport {
    private final AccessManagementService accessManagementService;
    private final ActorResolver actorResolver;

    public AccessManagementController(AccessManagementService accessManagementService, ActorResolver actorResolver) {
        this.accessManagementService = accessManagementService;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/me/effective-access")
    public ResponseEntity<ApiResponse<EffectiveAccessResponse>> effectiveAccess(HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Effective access fetched successfully", accessManagementService.effectiveAccess(organizationId, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<AccessAssignmentResponse>>> assignments(HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Access assignments fetched successfully", accessManagementService.assignments(organizationId, actorResolver.requireActor(null, request)), request);
    }

    @PostMapping("/applications/{applicationId}/invitations")
    public ResponseEntity<ApiResponse<ApplicationInvitationResponse>> inviteApplicationUser(@PathVariable String applicationId,
                                                                                            @Valid @RequestBody ApplicationInvitationCreateRequest body,
                                                                                            HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return created("Application invitation created successfully", accessManagementService.inviteApplicationUser(organizationId, applicationId, body, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/invitations")
    public ResponseEntity<ApiResponse<List<ApplicationInvitationResponse>>> invitations(HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Application invitations fetched successfully", accessManagementService.invitations(organizationId, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/invitations/me")
    public ResponseEntity<ApiResponse<List<ApplicationInvitationResponse>>> myInvitations(HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("My application invitations fetched successfully", accessManagementService.myInvitations(organizationId, actorResolver.requireActor(null, request)), request);
    }

    @PostMapping("/invitations/{id}/accept")
    public ResponseEntity<ApiResponse<ApplicationInvitationResponse>> acceptInvitation(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Application invitation accepted successfully", accessManagementService.acceptInvitation(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PostMapping("/invitations/{id}/reject")
    public ResponseEntity<ApiResponse<ApplicationInvitationResponse>> rejectInvitation(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Application invitation rejected successfully", accessManagementService.rejectInvitation(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PostMapping("/invitations/{id}/revoke")
    public ResponseEntity<ApiResponse<ApplicationInvitationResponse>> revokeInvitation(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Application invitation revoked successfully", accessManagementService.revokeInvitation(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PostMapping("/teams")
    public ResponseEntity<ApiResponse<AccessTeamResponse>> createTeam(@Valid @RequestBody AccessTeamCreateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return created("Access team created successfully", accessManagementService.createTeam(organizationId, body, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/teams")
    public ResponseEntity<ApiResponse<List<AccessTeamResponse>>> teams(HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Access teams fetched successfully", accessManagementService.teams(organizationId, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/teams/{id}")
    public ResponseEntity<ApiResponse<AccessTeamResponse>> team(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Access team fetched successfully", accessManagementService.team(organizationId, id, actorResolver.requireActor(null, request)), request);
    }


    @PatchMapping("/teams/{id}")
    public ResponseEntity<ApiResponse<AccessTeamResponse>> updateTeam(@PathVariable String id,
                                                                      @Valid @RequestBody AccessTeamUpdateRequest body,
                                                                      HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Access team updated successfully", accessManagementService.updateTeam(organizationId, id, body, actorResolver.requireActor(null, request)), request);
    }

    @DeleteMapping("/teams/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        accessManagementService.deleteTeam(organizationId, id, actorResolver.requireActor(null, request));
        return noData("Access team deleted successfully", request);
    }
    @PostMapping("/teams/{id}/members/invitations")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> inviteTeamMember(@PathVariable String id,
                                                                                @Valid @RequestBody TeamInvitationCreateRequest body,
                                                                                HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return created("Team invitation created successfully", accessManagementService.inviteTeamMember(organizationId, id, body, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/teams/invitations")
    public ResponseEntity<ApiResponse<List<TeamInvitationResponse>>> teamInvitations(HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Team invitations fetched successfully", accessManagementService.teamInvitations(organizationId, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/teams/invitations/me")
    public ResponseEntity<ApiResponse<List<TeamInvitationResponse>>> myTeamInvitations(HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("My team invitations fetched successfully", accessManagementService.myTeamInvitations(organizationId, actorResolver.requireActor(null, request)), request);
    }

    @PostMapping("/teams/invitations/{id}/accept")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> acceptTeamInvitation(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Team invitation accepted successfully", accessManagementService.acceptTeamInvitation(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PostMapping("/teams/invitations/{id}/reject")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> rejectTeamInvitation(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Team invitation rejected successfully", accessManagementService.rejectTeamInvitation(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PostMapping("/teams/invitations/{id}/revoke")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> revokeTeamInvitation(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Team invitation revoked successfully", accessManagementService.revokeTeamInvitation(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PutMapping("/teams/{teamId}/applications/{applicationId}")
    public ResponseEntity<ApiResponse<AccessTeamResponse>> grantTeamApplication(@PathVariable String teamId,
                                                                                @PathVariable String applicationId,
                                                                                HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Team application access granted successfully", accessManagementService.grantTeamToApplication(organizationId, teamId, applicationId, actorResolver.requireActor(null, request)), request);
    }

    @DeleteMapping("/teams/{teamId}/applications/{applicationId}")
    public ResponseEntity<ApiResponse<AccessTeamResponse>> revokeTeamApplication(@PathVariable String teamId,
                                                                                 @PathVariable String applicationId,
                                                                                 HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Team application access revoked successfully", accessManagementService.revokeTeamFromApplication(organizationId, teamId, applicationId, actorResolver.requireActor(null, request)), request);
    }
}

