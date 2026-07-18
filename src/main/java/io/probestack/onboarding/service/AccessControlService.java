package io.probestack.onboarding.service;

import io.probestack.onboarding.exception.ForbiddenOperationException;
import io.probestack.onboarding.model.*;
import io.probestack.onboarding.repository.*;
import io.probestack.onboarding.util.ActorResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccessControlService {
    private final AccessAssignmentRepository assignmentRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final ProjectRepository projectRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationInvitationRepository applicationInvitationRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamApplicationGrantRepository teamGrantRepository;
    private final ApplicationConsumerLinkRepository linkRepository;
    private final Set<String> bootstrapOrgAdminEmails;

    public AccessControlService(AccessAssignmentRepository assignmentRepository,
                                BusinessUnitRepository businessUnitRepository,
                                ProjectRepository projectRepository,
                                ApplicationRepository applicationRepository,
                                ApplicationInvitationRepository applicationInvitationRepository,
                                TeamInvitationRepository teamInvitationRepository,
                                TeamApplicationGrantRepository teamGrantRepository,
                                ApplicationConsumerLinkRepository linkRepository,
                                @Value("${onboarding.access.org-admin-emails:}") String orgAdminEmails) {
        this.assignmentRepository = assignmentRepository;
        this.businessUnitRepository = businessUnitRepository;
        this.projectRepository = projectRepository;
        this.applicationRepository = applicationRepository;
        this.applicationInvitationRepository = applicationInvitationRepository;
        this.teamInvitationRepository = teamInvitationRepository;
        this.teamGrantRepository = teamGrantRepository;
        this.linkRepository = linkRepository;
        this.bootstrapOrgAdminEmails = Arrays.stream(orgAdminEmails.split(","))
                .map(this::normalizeEmail)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    public EffectiveAccess effectiveAccess(String organizationId, ActorResolver.Actor actor) {
        String email = normalizeEmail(actor == null ? null : actor.email());
        if (!StringUtils.hasText(email)) {
            throw new ForbiddenOperationException("X-User-Email header is required for onboarding access");
        }
        List<BusinessUnit> businessUnits = businessUnitRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId);
        List<OnboardingProject> projects = projectRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId);
        List<OnboardingApplication> applications = applicationRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId);
        EffectiveAccess access = new EffectiveAccess(organizationId, email);
        if (bootstrapOrgAdminEmails.contains(email)) {
            access.orgAdmin = true;
        }

        for (AccessAssignment assignment : assignmentRepository.findByOrganizationIdAndPrincipalEmailIgnoreCaseAndActiveTrue(organizationId, email)) {
            applyAssignment(access, assignment, projects, applications);
        }
        for (BusinessUnit unit : businessUnits) {
            if (emailEquals(email, unit.getOwnerEmail())) addBusinessUnitAdmin(access, unit.getId(), projects, applications);
        }
        for (OnboardingProject project : projects) {
            if (emailEquals(email, project.getOwnerEmail())) addProjectAdmin(access, project.getId(), applications);
        }
        for (OnboardingApplication app : applications) {
            if (emailEquals(email, app.getOwnerEmail())) addApplicationOwner(access, app.getId());
        }
        for (ApplicationInvitation invitation : applicationInvitationRepository.findByOrganizationIdAndStatusAndAcceptedByEmailIgnoreCaseOrderByCreatedAtDesc(organizationId, InvitationStatus.ACCEPTED, email)) {
            access.viewApplicationIds.add(invitation.getApplicationId());
            access.memberApplicationIds.add(invitation.getApplicationId());
        }
        List<String> teamIds = teamInvitationRepository.findByOrganizationIdAndStatusAndAcceptedByEmailIgnoreCaseOrderByCreatedAtDesc(organizationId, InvitationStatus.ACCEPTED, email)
                .stream().map(TeamInvitation::getTeamId).distinct().toList();
        if (!teamIds.isEmpty()) {
            for (TeamApplicationGrant grant : teamGrantRepository.findByOrganizationIdAndTeamIdIn(organizationId, teamIds)) {
                access.viewApplicationIds.add(grant.getApplicationId());
                access.memberApplicationIds.add(grant.getApplicationId());
            }
        }
        expandViewParents(access, projects, applications);
        if (access.orgAdmin) {
            access.manageBusinessUnitIds.addAll(businessUnits.stream().map(BusinessUnit::getId).collect(Collectors.toSet()));
            access.viewBusinessUnitIds.addAll(access.manageBusinessUnitIds);
            access.manageProjectIds.addAll(projects.stream().map(OnboardingProject::getId).collect(Collectors.toSet()));
            access.viewProjectIds.addAll(access.manageProjectIds);
            access.manageApplicationIds.addAll(applications.stream().map(OnboardingApplication::getId).collect(Collectors.toSet()));
            access.viewApplicationIds.addAll(access.manageApplicationIds);
        }
        return access;
    }

    public boolean canViewBusinessUnit(String organizationId, String id, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        return access.orgAdmin || access.viewBusinessUnitIds.contains(id) || access.manageBusinessUnitIds.contains(id);
    }

    public boolean canManageBusinessUnit(String organizationId, String id, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        return access.orgAdmin || access.manageBusinessUnitIds.contains(id);
    }

    public boolean canViewProject(String organizationId, String id, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        return access.orgAdmin || access.viewProjectIds.contains(id) || access.manageProjectIds.contains(id);
    }

    public boolean canManageProject(String organizationId, String id, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        return access.orgAdmin || access.manageProjectIds.contains(id);
    }

    public boolean canViewApplication(String organizationId, String id, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        return access.orgAdmin || access.viewApplicationIds.contains(id) || access.manageApplicationIds.contains(id);
    }

    public boolean canManageApplication(String organizationId, String id, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        return access.orgAdmin || access.manageApplicationIds.contains(id);
    }

    public boolean canManageConsumerCatalog(String organizationId, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        return access.orgAdmin || !access.manageBusinessUnitIds.isEmpty() || !access.manageProjectIds.isEmpty() || !access.manageApplicationIds.isEmpty();
    }

    public boolean canViewConsumer(String organizationId, String consumerId, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        if (access.orgAdmin || canManageConsumerCatalog(organizationId, actor)) return true;
        Set<String> visibleApps = access.allVisibleApplications();
        if (visibleApps.isEmpty()) return false;
        return linkRepository.findByOrganizationIdAndApplicationIdIn(organizationId, visibleApps.stream().toList())
                .stream().anyMatch(link -> consumerId.equals(link.getConsumerId()));
    }

    public void requireOrgAdmin(String organizationId, ActorResolver.Actor actor) {
        if (!effectiveAccess(organizationId, actor).orgAdmin) throw forbidden();
    }

    public void requireBusinessUnitView(String organizationId, String id, ActorResolver.Actor actor) {
        if (!canViewBusinessUnit(organizationId, id, actor)) throw forbidden();
    }

    public void requireBusinessUnitManage(String organizationId, String id, ActorResolver.Actor actor) {
        if (!canManageBusinessUnit(organizationId, id, actor)) throw forbidden();
    }

    public void requireProjectView(String organizationId, String id, ActorResolver.Actor actor) {
        if (!canViewProject(organizationId, id, actor)) throw forbidden();
    }

    public void requireProjectManage(String organizationId, String id, ActorResolver.Actor actor) {
        if (!canManageProject(organizationId, id, actor)) throw forbidden();
    }

    public void requireApplicationView(String organizationId, String id, ActorResolver.Actor actor) {
        if (!canViewApplication(organizationId, id, actor)) throw forbidden();
    }

    public void requireApplicationManage(String organizationId, String id, ActorResolver.Actor actor) {
        if (!canManageApplication(organizationId, id, actor)) throw forbidden();
    }

    public void requireConsumerView(String organizationId, String id, ActorResolver.Actor actor) {
        if (!canViewConsumer(organizationId, id, actor)) throw forbidden();
    }

    public void requireConsumerManage(String organizationId, String id, ActorResolver.Actor actor) {
        if (!canManageConsumerCatalog(organizationId, actor)) throw forbidden();
    }

    public List<BusinessUnit> filterBusinessUnits(String organizationId, List<BusinessUnit> units, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        if (access.orgAdmin) return units;
        return units.stream().filter(unit -> access.viewBusinessUnitIds.contains(unit.getId()) || access.manageBusinessUnitIds.contains(unit.getId())).toList();
    }

    public List<OnboardingProject> filterProjects(String organizationId, List<OnboardingProject> projects, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        if (access.orgAdmin) return projects;
        return projects.stream().filter(project -> access.viewProjectIds.contains(project.getId()) || access.manageProjectIds.contains(project.getId())).toList();
    }

    public List<OnboardingApplication> filterApplications(String organizationId, List<OnboardingApplication> applications, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        if (access.orgAdmin) return applications;
        return applications.stream().filter(app -> access.viewApplicationIds.contains(app.getId()) || access.manageApplicationIds.contains(app.getId())).toList();
    }

    public List<Consumer> filterConsumers(String organizationId, List<Consumer> consumers, ActorResolver.Actor actor) {
        EffectiveAccess access = effectiveAccess(organizationId, actor);
        if (access.orgAdmin || !access.manageBusinessUnitIds.isEmpty() || !access.manageProjectIds.isEmpty() || !access.manageApplicationIds.isEmpty()) return consumers;
        Set<String> visibleApps = access.allVisibleApplications();
        if (visibleApps.isEmpty()) return List.of();
        Set<String> visibleConsumerIds = linkRepository.findByOrganizationIdAndApplicationIdIn(organizationId, visibleApps.stream().toList())
                .stream().map(ApplicationConsumerLink::getConsumerId).collect(Collectors.toSet());
        return consumers.stream().filter(consumer -> visibleConsumerIds.contains(consumer.getId())).toList();
    }

    private void applyAssignment(EffectiveAccess access, AccessAssignment assignment, List<OnboardingProject> projects, List<OnboardingApplication> apps) {
        if (assignment.getRole() == AccessRole.ORG_ADMIN && assignment.getScopeType() == AccessScopeType.ORGANIZATION) {
            access.orgAdmin = true;
            return;
        }
        if (assignment.getRole() == AccessRole.BUSINESS_UNIT_ADMIN && assignment.getScopeType() == AccessScopeType.BUSINESS_UNIT) {
            addBusinessUnitAdmin(access, assignment.getScopeId(), projects, apps);
        } else if (assignment.getRole() == AccessRole.PROJECT_ADMIN && assignment.getScopeType() == AccessScopeType.PROJECT) {
            addProjectAdmin(access, assignment.getScopeId(), apps);
        } else if (assignment.getRole() == AccessRole.APPLICATION_OWNER && assignment.getScopeType() == AccessScopeType.APPLICATION) {
            addApplicationOwner(access, assignment.getScopeId());
        } else if (assignment.getRole() == AccessRole.APPLICATION_MEMBER && assignment.getScopeType() == AccessScopeType.APPLICATION) {
            access.viewApplicationIds.add(assignment.getScopeId());
            access.memberApplicationIds.add(assignment.getScopeId());
        }
    }

    private void addBusinessUnitAdmin(EffectiveAccess access, String businessUnitId, List<OnboardingProject> projects, List<OnboardingApplication> apps) {
        access.manageBusinessUnitIds.add(businessUnitId);
        access.viewBusinessUnitIds.add(businessUnitId);
        projects.stream().filter(project -> businessUnitId.equals(project.getBusinessUnitId())).forEach(project -> addProjectAdmin(access, project.getId(), apps));
    }

    private void addProjectAdmin(EffectiveAccess access, String projectId, List<OnboardingApplication> apps) {
        access.manageProjectIds.add(projectId);
        access.viewProjectIds.add(projectId);
        apps.stream().filter(app -> projectId.equals(app.getProjectId())).forEach(app -> addApplicationOwner(access, app.getId()));
    }

    private void addApplicationOwner(EffectiveAccess access, String applicationId) {
        access.manageApplicationIds.add(applicationId);
        access.viewApplicationIds.add(applicationId);
    }

    private void expandViewParents(EffectiveAccess access, List<OnboardingProject> projects, List<OnboardingApplication> apps) {
        for (OnboardingApplication app : apps) {
            if (access.viewApplicationIds.contains(app.getId()) || access.manageApplicationIds.contains(app.getId())) {
                access.viewProjectIds.add(app.getProjectId());
                access.viewBusinessUnitIds.add(app.getBusinessUnitId());
            }
        }
        for (OnboardingProject project : projects) {
            if (access.viewProjectIds.contains(project.getId()) || access.manageProjectIds.contains(project.getId())) {
                access.viewBusinessUnitIds.add(project.getBusinessUnitId());
            }
        }
    }

    private ForbiddenOperationException forbidden() {
        return new ForbiddenOperationException("You do not have access to perform this onboarding action");
    }

    private boolean emailEquals(String left, String right) {
        return StringUtils.hasText(left) && left.equals(normalizeEmail(right));
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    public static class EffectiveAccess {
        public final String organizationId;
        public final String userEmail;
        public boolean orgAdmin;
        public final Set<String> viewBusinessUnitIds = new HashSet<>();
        public final Set<String> manageBusinessUnitIds = new HashSet<>();
        public final Set<String> viewProjectIds = new HashSet<>();
        public final Set<String> manageProjectIds = new HashSet<>();
        public final Set<String> viewApplicationIds = new HashSet<>();
        public final Set<String> manageApplicationIds = new HashSet<>();
        public final Set<String> memberApplicationIds = new HashSet<>();

        public EffectiveAccess(String organizationId, String userEmail) {
            this.organizationId = organizationId;
            this.userEmail = userEmail;
        }

        public Set<String> allVisibleApplications() {
            Set<String> ids = new HashSet<>(viewApplicationIds);
            ids.addAll(manageApplicationIds);
            ids.addAll(memberApplicationIds);
            return ids;
        }
    }
}

