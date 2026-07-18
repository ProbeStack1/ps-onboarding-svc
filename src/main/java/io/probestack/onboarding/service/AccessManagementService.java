package io.probestack.onboarding.service;

import io.probestack.onboarding.dto.access.*;
import io.probestack.onboarding.exception.DuplicateResourceException;
import io.probestack.onboarding.exception.ResourceNotFoundException;
import io.probestack.onboarding.model.*;
import io.probestack.onboarding.repository.*;
import io.probestack.onboarding.util.ActorResolver;
import io.probestack.onboarding.util.SlugNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
public class AccessManagementService {
    private final AccessControlService accessControlService;
    private final AccessAssignmentRepository assignmentRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationInvitationRepository invitationRepository;
    private final AccessTeamRepository teamRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamApplicationGrantRepository teamGrantRepository;
    private final AuditService auditService;

    public AccessManagementService(AccessControlService accessControlService,
                                   AccessAssignmentRepository assignmentRepository,
                                   ApplicationRepository applicationRepository,
                                   ApplicationInvitationRepository invitationRepository,
                                   AccessTeamRepository teamRepository,
                                   TeamInvitationRepository teamInvitationRepository,
                                   TeamApplicationGrantRepository teamGrantRepository,
                                   AuditService auditService) {
        this.accessControlService = accessControlService;
        this.assignmentRepository = assignmentRepository;
        this.applicationRepository = applicationRepository;
        this.invitationRepository = invitationRepository;
        this.teamRepository = teamRepository;
        this.teamInvitationRepository = teamInvitationRepository;
        this.teamGrantRepository = teamGrantRepository;
        this.auditService = auditService;
    }

    public EffectiveAccessResponse effectiveAccess(String organizationId, ActorResolver.Actor actor) {
        var access = accessControlService.effectiveAccess(organizationId, actor);
        return EffectiveAccessResponse.builder()
                .organizationId(access.organizationId)
                .userEmail(access.userEmail)
                .orgAdmin(access.orgAdmin)
                .viewBusinessUnitIds(access.viewBusinessUnitIds)
                .manageBusinessUnitIds(access.manageBusinessUnitIds)
                .viewProjectIds(access.viewProjectIds)
                .manageProjectIds(access.manageProjectIds)
                .viewApplicationIds(access.viewApplicationIds)
                .manageApplicationIds(access.manageApplicationIds)
                .memberApplicationIds(access.memberApplicationIds)
                .build();
    }

    public List<AccessAssignmentResponse> assignments(String organizationId, ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        return assignmentRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream().map(this::toAssignmentResponse).toList();
    }

    public ApplicationInvitationResponse inviteApplicationUser(String organizationId, String applicationId, ApplicationInvitationCreateRequest request, ActorResolver.Actor actor) {
        OnboardingApplication app = findApplication(organizationId, applicationId);
        accessControlService.requireApplicationManage(organizationId, applicationId, actor);
        ApplicationInvitation invitation = ApplicationInvitation.builder()
                .organizationId(organizationId)
                .applicationId(applicationId)
                .invitedEmail(normalizeEmail(request.getInvitedEmail()))
                .invitedName(SlugNormalizer.trimToNull(request.getInvitedName()))
                .message(SlugNormalizer.trimToNull(request.getMessage()))
                .role(AccessRole.APPLICATION_MEMBER)
                .status(InvitationStatus.PENDING)
                .createdBy(actor.name())
                .createdByEmail(actor.email())
                .build();
        ApplicationInvitation saved = invitationRepository.save(invitation);
        auditService.record(organizationId, ResourceType.APPLICATION_INVITATION, saved.getId(), AuditAction.INVITE_APPLICATION_USER, actor, List.of(), null, saved);
        return toInvitationResponse(saved, app);
    }

    public List<ApplicationInvitationResponse> invitations(String organizationId, ActorResolver.Actor actor) {
        var access = accessControlService.effectiveAccess(organizationId, actor);
        return invitationRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .filter(invitation -> access.orgAdmin || access.manageApplicationIds.contains(invitation.getApplicationId()) || actor.email().equalsIgnoreCase(invitation.getInvitedEmail()))
                .map(invitation -> toInvitationResponse(invitation, findApplicationOrNull(organizationId, invitation.getApplicationId())))
                .toList();
    }

    public List<ApplicationInvitationResponse> myInvitations(String organizationId, ActorResolver.Actor actor) {
        return invitationRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING || (StringUtils.hasText(actor.email()) && actor.email().equalsIgnoreCase(invitation.getInvitedEmail())) || actor.email().equalsIgnoreCase(nullToEmpty(invitation.getAcceptedByEmail())))
                .map(invitation -> toInvitationResponse(invitation, findApplicationOrNull(organizationId, invitation.getApplicationId())))
                .toList();
    }

    public ApplicationInvitationResponse acceptInvitation(String organizationId, String id, ActorResolver.Actor actor) {
        ApplicationInvitation invitation = findInvitation(organizationId, id);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedByEmail(actor.email());
        invitation.setAcceptedAt(Instant.now());
        ApplicationInvitation saved = invitationRepository.save(invitation);
        auditService.record(organizationId, ResourceType.APPLICATION_INVITATION, saved.getId(), AuditAction.ACCEPT_APPLICATION_INVITATION, actor, List.of(), null, saved);
        return toInvitationResponse(saved, findApplicationOrNull(organizationId, saved.getApplicationId()));
    }

    public ApplicationInvitationResponse rejectInvitation(String organizationId, String id, ActorResolver.Actor actor) {
        ApplicationInvitation invitation = findInvitation(organizationId, id);
        invitation.setStatus(InvitationStatus.REJECTED);
        invitation.setRejectedByEmail(actor.email());
        invitation.setRejectedAt(Instant.now());
        ApplicationInvitation saved = invitationRepository.save(invitation);
        auditService.record(organizationId, ResourceType.APPLICATION_INVITATION, saved.getId(), AuditAction.REJECT_APPLICATION_INVITATION, actor, List.of(), null, saved);
        return toInvitationResponse(saved, findApplicationOrNull(organizationId, saved.getApplicationId()));
    }

    public ApplicationInvitationResponse revokeInvitation(String organizationId, String id, ActorResolver.Actor actor) {
        ApplicationInvitation invitation = findInvitation(organizationId, id);
        accessControlService.requireApplicationManage(organizationId, invitation.getApplicationId(), actor);
        invitation.setStatus(InvitationStatus.REVOKED);
        invitation.setRevokedByEmail(actor.email());
        invitation.setRevokedAt(Instant.now());
        ApplicationInvitation saved = invitationRepository.save(invitation);
        auditService.record(organizationId, ResourceType.APPLICATION_INVITATION, saved.getId(), AuditAction.REVOKE_APPLICATION_INVITATION, actor, List.of(), null, saved);
        return toInvitationResponse(saved, findApplicationOrNull(organizationId, saved.getApplicationId()));
    }

    public AccessTeamResponse createTeam(String organizationId, AccessTeamCreateRequest request, ActorResolver.Actor actor) {
        String applicationId = SlugNormalizer.trimToNull(request.getApplicationId());
        OnboardingApplication app = null;
        if (StringUtils.hasText(applicationId)) {
            app = findApplication(organizationId, applicationId);
            accessControlService.requireApplicationManage(organizationId, applicationId, actor);
        } else if (!accessControlService.canManageConsumerCatalog(organizationId, actor)) {
            accessControlService.requireOrgAdmin(organizationId, actor);
        }
        AccessTeam team = AccessTeam.builder()
                .organizationId(organizationId)
                .name(request.getName().trim())
                .description(SlugNormalizer.trimToNull(request.getDescription()))
                .businessUnitId(app == null ? null : app.getBusinessUnitId())
                .projectId(app == null ? null : app.getProjectId())
                .applicationId(app == null ? null : app.getId())
                .createdBy(actor.name())
                .createdByEmail(actor.email())
                .build();
        AccessTeam saved = teamRepository.save(team);
        auditService.record(organizationId, ResourceType.ACCESS_TEAM, saved.getId(), AuditAction.CREATE_TEAM, actor, List.of(), null, saved);
        if (app != null) grantTeamToApplication(organizationId, saved.getId(), app.getId(), actor);
        return toTeamResponse(saved);
    }

    public List<AccessTeamResponse> teams(String organizationId, ActorResolver.Actor actor) {
        var access = accessControlService.effectiveAccess(organizationId, actor);
        return teamRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId).stream()
                .filter(team -> access.orgAdmin || canManageTeam(access, team, actor.email()) || isAcceptedTeamMember(organizationId, team.getId(), actor.email()))
                .map(this::toTeamResponse)
                .toList();
    }

    public AccessTeamResponse team(String organizationId, String id, ActorResolver.Actor actor) {
        AccessTeam team = findTeam(organizationId, id);
        var access = accessControlService.effectiveAccess(organizationId, actor);
        if (!access.orgAdmin && !canManageTeam(access, team, actor.email()) && !isAcceptedTeamMember(organizationId, id, actor.email())) {
            throw new io.probestack.onboarding.exception.ForbiddenOperationException("You do not have access to this team");
        }
        return toTeamResponse(team);
    }


    public AccessTeamResponse updateTeam(String organizationId, String id, AccessTeamUpdateRequest request, ActorResolver.Actor actor) {
        AccessTeam team = findTeam(organizationId, id);
        var access = accessControlService.effectiveAccess(organizationId, actor);
        if (!access.orgAdmin && !canManageTeam(access, team, actor.email())) {
            throw new io.probestack.onboarding.exception.ForbiddenOperationException("You do not have access to manage this team");
        }
        java.util.Map<String, Object> before = auditService.toMap(team);
        team.setName(request.getName().trim());
        team.setDescription(SlugNormalizer.trimToNull(request.getDescription()));
        team.setUpdatedBy(actor.name());
        team.setUpdatedByEmail(actor.email());
        AccessTeam saved = teamRepository.save(team);
        auditService.record(organizationId, ResourceType.ACCESS_TEAM, saved.getId(), AuditAction.UPDATE, actor, List.of(), before, saved);
        return toTeamResponse(saved);
    }

    public void deleteTeam(String organizationId, String id, ActorResolver.Actor actor) {
        AccessTeam team = findTeam(organizationId, id);
        var access = accessControlService.effectiveAccess(organizationId, actor);
        if (!access.orgAdmin && !canManageTeam(access, team, actor.email())) {
            throw new io.probestack.onboarding.exception.ForbiddenOperationException("You do not have access to manage this team");
        }
        java.util.Map<String, Object> before = auditService.toMap(team);
        team.setDeletedAt(Instant.now());
        team.setDeletedBy(actor.name());
        team.setDeletedByEmail(actor.email());
        team.setUpdatedBy(actor.name());
        team.setUpdatedByEmail(actor.email());
        AccessTeam saved = teamRepository.save(team);
        teamGrantRepository.deleteByOrganizationIdAndTeamId(organizationId, id);
        auditService.record(organizationId, ResourceType.ACCESS_TEAM, saved.getId(), AuditAction.DELETE, actor, List.of(), before, saved);
    }
    public TeamInvitationResponse inviteTeamMember(String organizationId, String teamId, TeamInvitationCreateRequest request, ActorResolver.Actor actor) {
        AccessTeam team = findTeam(organizationId, teamId);
        var access = accessControlService.effectiveAccess(organizationId, actor);
        if (!access.orgAdmin && !canManageTeam(access, team, actor.email())) {
            throw new io.probestack.onboarding.exception.ForbiddenOperationException("You do not have access to manage this team");
        }
        TeamInvitation invitation = TeamInvitation.builder()
                .organizationId(organizationId)
                .teamId(teamId)
                .invitedEmail(normalizeEmail(request.getInvitedEmail()))
                .invitedName(SlugNormalizer.trimToNull(request.getInvitedName()))
                .status(InvitationStatus.PENDING)
                .createdBy(actor.name())
                .createdByEmail(actor.email())
                .build();
        TeamInvitation saved = teamInvitationRepository.save(invitation);
        auditService.record(organizationId, ResourceType.TEAM_INVITATION, saved.getId(), AuditAction.INVITE_TEAM_MEMBER, actor, List.of(), null, saved);
        return toTeamInvitationResponse(saved, team);
    }

    public List<TeamInvitationResponse> teamInvitations(String organizationId, ActorResolver.Actor actor) {
        var access = accessControlService.effectiveAccess(organizationId, actor);
        return teamInvitationRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .filter(invitation -> {
                    AccessTeam team = findTeamOrNull(organizationId, invitation.getTeamId());
                    return access.orgAdmin || (team != null && canManageTeam(access, team, actor.email())) || actor.email().equalsIgnoreCase(invitation.getInvitedEmail());
                })
                .map(invitation -> toTeamInvitationResponse(invitation, findTeamOrNull(organizationId, invitation.getTeamId())))
                .toList();
    }

    public List<TeamInvitationResponse> myTeamInvitations(String organizationId, ActorResolver.Actor actor) {
        return teamInvitationRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING || actor.email().equalsIgnoreCase(invitation.getInvitedEmail()) || actor.email().equalsIgnoreCase(nullToEmpty(invitation.getAcceptedByEmail())))
                .map(invitation -> toTeamInvitationResponse(invitation, findTeamOrNull(organizationId, invitation.getTeamId())))
                .toList();
    }

    public TeamInvitationResponse acceptTeamInvitation(String organizationId, String id, ActorResolver.Actor actor) {
        TeamInvitation invitation = findTeamInvitation(organizationId, id);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedByEmail(actor.email());
        invitation.setAcceptedAt(Instant.now());
        TeamInvitation saved = teamInvitationRepository.save(invitation);
        auditService.record(organizationId, ResourceType.TEAM_INVITATION, saved.getId(), AuditAction.ACCEPT_TEAM_INVITATION, actor, List.of(), null, saved);
        return toTeamInvitationResponse(saved, findTeamOrNull(organizationId, saved.getTeamId()));
    }

    public TeamInvitationResponse rejectTeamInvitation(String organizationId, String id, ActorResolver.Actor actor) {
        TeamInvitation invitation = findTeamInvitation(organizationId, id);
        invitation.setStatus(InvitationStatus.REJECTED);
        invitation.setRejectedByEmail(actor.email());
        invitation.setRejectedAt(Instant.now());
        TeamInvitation saved = teamInvitationRepository.save(invitation);
        auditService.record(organizationId, ResourceType.TEAM_INVITATION, saved.getId(), AuditAction.REJECT_TEAM_INVITATION, actor, List.of(), null, saved);
        return toTeamInvitationResponse(saved, findTeamOrNull(organizationId, saved.getTeamId()));
    }

    public TeamInvitationResponse revokeTeamInvitation(String organizationId, String id, ActorResolver.Actor actor) {
        TeamInvitation invitation = findTeamInvitation(organizationId, id);
        AccessTeam team = findTeam(organizationId, invitation.getTeamId());
        var access = accessControlService.effectiveAccess(organizationId, actor);
        if (!access.orgAdmin && !canManageTeam(access, team, actor.email())) {
            throw new io.probestack.onboarding.exception.ForbiddenOperationException("You do not have access to manage this team");
        }
        invitation.setStatus(InvitationStatus.REVOKED);
        invitation.setRevokedByEmail(actor.email());
        invitation.setRevokedAt(Instant.now());
        TeamInvitation saved = teamInvitationRepository.save(invitation);
        auditService.record(organizationId, ResourceType.TEAM_INVITATION, saved.getId(), AuditAction.REVOKE_TEAM_INVITATION, actor, List.of(), null, saved);
        return toTeamInvitationResponse(saved, team);
    }

    public AccessTeamResponse grantTeamToApplication(String organizationId, String teamId, String applicationId, ActorResolver.Actor actor) {
        AccessTeam team = findTeam(organizationId, teamId);
        OnboardingApplication app = findApplication(organizationId, applicationId);
        accessControlService.requireApplicationManage(organizationId, applicationId, actor);
        var access = accessControlService.effectiveAccess(organizationId, actor);
        if (!access.orgAdmin && !canManageTeam(access, team, actor.email())) {
            throw new io.probestack.onboarding.exception.ForbiddenOperationException("You do not have access to manage this team");
        }
        if (teamGrantRepository.existsByOrganizationIdAndTeamIdAndApplicationId(organizationId, teamId, applicationId)) {
            throw new DuplicateResourceException("Team already has access to this application");
        }
        TeamApplicationGrant grant = teamGrantRepository.save(TeamApplicationGrant.builder()
                .organizationId(organizationId)
                .teamId(teamId)
                .applicationId(app.getId())
                .createdBy(actor.name())
                .createdByEmail(actor.email())
                .build());
        auditService.record(organizationId, ResourceType.TEAM_APPLICATION_GRANT, grant.getId(), AuditAction.GRANT_TEAM_APPLICATION, actor, List.of(), null, grant);
        return toTeamResponse(team);
    }

    public AccessTeamResponse revokeTeamFromApplication(String organizationId, String teamId, String applicationId, ActorResolver.Actor actor) {
        AccessTeam team = findTeam(organizationId, teamId);
        accessControlService.requireApplicationManage(organizationId, applicationId, actor);
        teamGrantRepository.deleteByOrganizationIdAndTeamIdAndApplicationId(organizationId, teamId, applicationId);
        auditService.record(organizationId, ResourceType.TEAM_APPLICATION_GRANT, teamId + ":" + applicationId, AuditAction.REVOKE_TEAM_APPLICATION, actor, List.of(), null, java.util.Map.of("teamId", teamId, "applicationId", applicationId));
        return toTeamResponse(team);
    }

    private AccessAssignmentResponse toAssignmentResponse(AccessAssignment assignment) {
        return AccessAssignmentResponse.builder()
                .id(assignment.getId())
                .principalEmail(assignment.getPrincipalEmail())
                .principalName(assignment.getPrincipalName())
                .role(assignment.getRole())
                .scopeType(assignment.getScopeType())
                .scopeId(assignment.getScopeId())
                .active(assignment.isActive())
                .createdAt(assignment.getCreatedAt())
                .build();
    }

    private ApplicationInvitationResponse toInvitationResponse(ApplicationInvitation invitation, OnboardingApplication app) {
        return ApplicationInvitationResponse.builder()
                .id(invitation.getId())
                .applicationId(invitation.getApplicationId())
                .applicationName(app == null ? null : app.getName())
                .invitedEmail(invitation.getInvitedEmail())
                .invitedName(invitation.getInvitedName())
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .message(invitation.getMessage())
                .createdByEmail(invitation.getCreatedByEmail())
                .acceptedByEmail(invitation.getAcceptedByEmail())
                .acceptedAt(invitation.getAcceptedAt())
                .rejectedByEmail(invitation.getRejectedByEmail())
                .rejectedAt(invitation.getRejectedAt())
                .revokedByEmail(invitation.getRevokedByEmail())
                .revokedAt(invitation.getRevokedAt())
                .createdAt(invitation.getCreatedAt())
                .build();
    }

    private AccessTeamResponse toTeamResponse(AccessTeam team) {
        var grants = teamGrantRepository.findByOrganizationIdAndTeamId(team.getOrganizationId(), team.getId());
        return AccessTeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .businessUnitId(team.getBusinessUnitId())
                .projectId(team.getProjectId())
                .applicationId(team.getApplicationId())
                .memberCount(teamInvitationRepository.findByOrganizationIdAndTeamIdOrderByCreatedAtDesc(team.getOrganizationId(), team.getId()).stream().filter(invite -> invite.getStatus() == InvitationStatus.ACCEPTED).count())
                .applicationCount(grants.size())
                .applicationIds(grants.stream().map(TeamApplicationGrant::getApplicationId).toList())
                .createdByEmail(team.getCreatedByEmail())
                .createdAt(team.getCreatedAt())
                .build();
    }

    private TeamInvitationResponse toTeamInvitationResponse(TeamInvitation invitation, AccessTeam team) {
        return TeamInvitationResponse.builder()
                .id(invitation.getId())
                .teamId(invitation.getTeamId())
                .teamName(team == null ? null : team.getName())
                .invitedEmail(invitation.getInvitedEmail())
                .invitedName(invitation.getInvitedName())
                .status(invitation.getStatus())
                .createdByEmail(invitation.getCreatedByEmail())
                .acceptedByEmail(invitation.getAcceptedByEmail())
                .acceptedAt(invitation.getAcceptedAt())
                .rejectedByEmail(invitation.getRejectedByEmail())
                .rejectedAt(invitation.getRejectedAt())
                .revokedByEmail(invitation.getRevokedByEmail())
                .revokedAt(invitation.getRevokedAt())
                .createdAt(invitation.getCreatedAt())
                .build();
    }

    private boolean canManageTeam(AccessControlService.EffectiveAccess access, AccessTeam team, String actorEmail) {
        if (StringUtils.hasText(actorEmail) && actorEmail.equalsIgnoreCase(nullToEmpty(team.getCreatedByEmail()))) {
            return true;
        }
        return (StringUtils.hasText(team.getApplicationId()) && access.manageApplicationIds.contains(team.getApplicationId()))
                || (StringUtils.hasText(team.getProjectId()) && access.manageProjectIds.contains(team.getProjectId()))
                || (StringUtils.hasText(team.getBusinessUnitId()) && access.manageBusinessUnitIds.contains(team.getBusinessUnitId()));
    }

    private boolean isAcceptedTeamMember(String organizationId, String teamId, String email) {
        return teamInvitationRepository.findByOrganizationIdAndTeamIdOrderByCreatedAtDesc(organizationId, teamId).stream()
                .anyMatch(invitation -> invitation.getStatus() == InvitationStatus.ACCEPTED && email.equalsIgnoreCase(nullToEmpty(invitation.getAcceptedByEmail())));
    }

    private OnboardingApplication findApplication(String organizationId, String applicationId) {
        return applicationRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(applicationId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
    }

    private OnboardingApplication findApplicationOrNull(String organizationId, String applicationId) {
        return applicationRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(applicationId, organizationId).orElse(null);
    }

    private ApplicationInvitation findInvitation(String organizationId, String id) {
        return invitationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application invitation not found: " + id));
    }

    private AccessTeam findTeam(String organizationId, String id) {
        return teamRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Access team not found: " + id));
    }

    private AccessTeam findTeamOrNull(String organizationId, String id) {
        return teamRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(id, organizationId).orElse(null);
    }

    private TeamInvitation findTeamInvitation(String organizationId, String id) {
        return teamInvitationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Team invitation not found: " + id));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

