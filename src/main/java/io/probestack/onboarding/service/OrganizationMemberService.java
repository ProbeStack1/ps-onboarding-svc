package io.probestack.onboarding.service;

import io.probestack.onboarding.client.OrganizationMemberClient;
import io.probestack.onboarding.client.OrganizationMemberRecord;
import io.probestack.onboarding.dto.access.EffectiveAccessResponse;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.dto.member.*;
import io.probestack.onboarding.exception.ForbiddenOperationException;
import io.probestack.onboarding.exception.ResourceNotFoundException;
import io.probestack.onboarding.model.OnboardingDeveloper;
import io.probestack.onboarding.model.RoleKind;
import io.probestack.onboarding.util.ActorResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrganizationMemberService {
    private final OrganizationMemberClient memberClient;
    private final MemberAccessResolver accessResolver;

    public OrganizationMemberService(
            OrganizationMemberClient memberClient,
            MemberAccessResolver accessResolver) {
        this.memberClient = memberClient;
        this.accessResolver = accessResolver;
    }

    public PagedResult<OrganizationMemberResponse> list(
            String organizationId,
            String search,
            String status,
            int page,
            int size,
            String authorization,
            ActorResolver.Actor actor) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        MemberAccessResolver.ResolutionContext context = accessResolver.loadContext(organizationId);
        requireDirectoryAdmin(context, organizationId, actor);
        OrganizationMemberClient.MemberPage members = memberClient.fetchMembers(
                organizationId, safePage, safeSize, search, status, authorization);
        List<OrganizationMemberResponse> responses = members.items().stream()
                .filter(member -> includeStatus(member, status))
                .map(member -> toSummary(context, organizationId, member))
                .toList();
        long total = responses.size() == members.items().size() ? members.totalElements() : responses.size();
        return PagedResult.<OrganizationMemberResponse>builder()
                .items(responses)
                .totalElements(total)
                .build();
    }

    public MemberAccessResponse get(
            String organizationId,
            String principalId,
            String authorization,
            ActorResolver.Actor actor) {
        MemberAccessResolver.ResolutionContext context = accessResolver.loadContext(organizationId);
        requireDirectoryAdmin(context, organizationId, actor);
        OrganizationMemberRecord member = memberClient.findMember(organizationId, principalId, authorization)
                .orElseThrow(() -> new ResourceNotFoundException("Organization member not found: " + principalId));
        MemberAccessResolver.Resolution resolution = resolve(context, organizationId, member);
        return MemberAccessResponse.builder()
                .member(toSummary(context, organizationId, member, resolution))
                .assignments(resolution.assignments())
                .effectiveAccess(toEffectiveAccess(organizationId, member.email(), resolution))
                .build();
    }

    private void requireDirectoryAdmin(
            MemberAccessResolver.ResolutionContext context,
            String organizationId,
            ActorResolver.Actor actor) {
        MemberAccessResolver.MemberIdentity identity = new MemberAccessResolver.MemberIdentity(
                StringUtils.hasText(actor.userId()) ? actor.userId() : actor.email(),
                organizationId,
                actor.email(),
                actor.name(),
                actor.role());
        if (!accessResolver.resolve(context, identity).orgAdmin()) {
            throw new ForbiddenOperationException("Only organization administrators can view the complete member directory");
        }
    }

    private OrganizationMemberResponse toSummary(
            MemberAccessResolver.ResolutionContext context,
            String organizationId,
            OrganizationMemberRecord member) {
        return toSummary(context, organizationId, member, resolve(context, organizationId, member));
    }

    private OrganizationMemberResponse toSummary(
            MemberAccessResolver.ResolutionContext context,
            String organizationId,
            OrganizationMemberRecord member,
            MemberAccessResolver.Resolution resolution) {
        OnboardingDeveloper profile = context.developers().stream()
                .filter(developer -> emailEquals(developer.getEmail(), member.email()))
                .findFirst()
                .orElse(null);
        Map<RoleSummaryKey, Long> grouped = resolution.assignments().stream()
                .filter(assignment -> assignment.getRoleKind() != RoleKind.ORGANIZATION)
                .collect(Collectors.groupingBy(
                        assignment -> new RoleSummaryKey(
                                assignment.getRoleKind(),
                                assignment.getRoleCode(),
                                assignment.getScopeType()),
                        LinkedHashMap::new,
                        Collectors.counting()));
        List<MemberRoleSummary> roleSummaries = grouped.entrySet().stream()
                .map(entry -> MemberRoleSummary.builder()
                        .roleKind(entry.getKey().roleKind())
                        .roleCode(entry.getKey().roleCode())
                        .scopeType(entry.getKey().scopeType())
                        .count(entry.getValue())
                        .build())
                .sorted(Comparator.comparing((MemberRoleSummary role) -> role.getRoleKind().ordinal())
                        .thenComparing(MemberRoleSummary::getRoleCode)
                        .thenComparing(MemberRoleSummary::getScopeType))
                .toList();
        return OrganizationMemberResponse.builder()
                .principalId(member.principalId())
                .organizationId(organizationId)
                .email(member.email())
                .name(member.name())
                .organizationRole(member.organizationRole())
                .accountStatus(member.accountStatus())
                .active(member.active())
                .developerProfileConfigured(profile != null)
                .developerProfileId(profile == null ? null : profile.getId())
                .developerRole(profile == null ? null : profile.getRole())
                .assignmentCount(resolution.assignments().stream()
                        .filter(assignment -> assignment.getRoleKind() != RoleKind.ORGANIZATION)
                        .count())
                .roles(roleSummaries)
                .build();
    }

    private MemberAccessResolver.Resolution resolve(
            MemberAccessResolver.ResolutionContext context,
            String organizationId,
            OrganizationMemberRecord member) {
        return accessResolver.resolve(context, new MemberAccessResolver.MemberIdentity(
                member.principalId(),
                organizationId,
                member.email(),
                member.name(),
                member.organizationRole()));
    }

    private EffectiveAccessResponse toEffectiveAccess(
            String organizationId,
            String email,
            MemberAccessResolver.Resolution resolution) {
        return EffectiveAccessResponse.builder()
                .organizationId(organizationId)
                .userEmail(email)
                .orgAdmin(resolution.orgAdmin())
                .viewBusinessUnitIds(resolution.viewBusinessUnitIds())
                .manageBusinessUnitIds(resolution.manageBusinessUnitIds())
                .viewProjectIds(resolution.viewProjectIds())
                .manageProjectIds(resolution.manageProjectIds())
                .viewApplicationIds(resolution.viewApplicationIds())
                .manageApplicationIds(resolution.manageApplicationIds())
                .memberApplicationIds(resolution.memberApplicationIds())
                .build();
    }

    private boolean includeStatus(OrganizationMemberRecord member, String requestedStatus) {
        if (!StringUtils.hasText(requestedStatus) || "ALL".equalsIgnoreCase(requestedStatus)) return true;
        if ("ACTIVE".equalsIgnoreCase(requestedStatus)) return member.active();
        return requestedStatus.equalsIgnoreCase(member.accountStatus());
    }

    private boolean emailEquals(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private record RoleSummaryKey(RoleKind roleKind, String roleCode, String scopeType) {
    }
}
