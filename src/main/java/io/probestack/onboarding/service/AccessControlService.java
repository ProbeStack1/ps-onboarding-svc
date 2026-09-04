package io.probestack.onboarding.service;

import io.probestack.onboarding.exception.ForbiddenOperationException;
import io.probestack.onboarding.model.*;
import io.probestack.onboarding.repository.*;
import io.probestack.onboarding.util.ActorResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccessControlService {
    private final ApplicationConsumerLinkRepository linkRepository;
    private final MemberAccessResolver memberAccessResolver;

    public AccessControlService(ApplicationConsumerLinkRepository linkRepository,
                                MemberAccessResolver memberAccessResolver) {
        this.linkRepository = linkRepository;
        this.memberAccessResolver = memberAccessResolver;
    }

    public EffectiveAccess effectiveAccess(String organizationId, ActorResolver.Actor actor) {
        String email = normalizeEmail(actor == null ? null : actor.email());
        if (!StringUtils.hasText(email)) {
            throw new ForbiddenOperationException("Authenticated token must contain an email claim for onboarding access");
        }
        MemberAccessResolver.ResolutionContext context = memberAccessResolver.loadContext(organizationId);
        MemberAccessResolver.Resolution resolution = memberAccessResolver.resolve(
                context,
                new MemberAccessResolver.MemberIdentity(
                        StringUtils.hasText(actor.userId()) ? actor.userId() : email,
                        organizationId,
                        email,
                        actor.name(),
                        actor.role()));
        EffectiveAccess access = new EffectiveAccess(organizationId, email);
        access.orgAdmin = resolution.orgAdmin();
        access.viewBusinessUnitIds.addAll(resolution.viewBusinessUnitIds());
        access.manageBusinessUnitIds.addAll(resolution.manageBusinessUnitIds());
        access.viewProjectIds.addAll(resolution.viewProjectIds());
        access.manageProjectIds.addAll(resolution.manageProjectIds());
        access.viewApplicationIds.addAll(resolution.viewApplicationIds());
        access.manageApplicationIds.addAll(resolution.manageApplicationIds());
        access.memberApplicationIds.addAll(resolution.memberApplicationIds());
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

    private ForbiddenOperationException forbidden() {
        return new ForbiddenOperationException("You do not have access to perform this onboarding action");
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

