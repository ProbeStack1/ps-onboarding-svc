package io.probestack.onboarding.service;

import io.probestack.onboarding.client.OrganizationMemberClient;
import io.probestack.onboarding.client.OrganizationMemberRecord;
import io.probestack.onboarding.dto.access.EffectiveAccessResponse;
import io.probestack.onboarding.dto.adminaccess.*;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.dto.member.MemberRoleAssignmentResponse;
import io.probestack.onboarding.dto.member.MemberRoleSummary;
import io.probestack.onboarding.dto.member.OrganizationMemberResponse;
import io.probestack.onboarding.exception.ResourceNotFoundException;
import io.probestack.onboarding.model.*;
import io.probestack.onboarding.util.ActorResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminAccessCatalogService {
    private static final int MEMBER_PAGE_SIZE = 200;
    private static final int MAX_MEMBER_PAGES = 1_000;

    private final OrganizationMemberClient memberClient;
    private final MemberAccessResolver accessResolver;

    public AdminAccessCatalogService(
            OrganizationMemberClient memberClient,
            MemberAccessResolver accessResolver) {
        this.memberClient = memberClient;
        this.accessResolver = accessResolver;
    }

    public PagedResult<AdminUserAccessResponse> listUsers(
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
        requireAdmin(context, organizationId, actor);
        OrganizationMemberClient.MemberPage memberPage = memberClient.fetchMembers(
                organizationId, safePage, safeSize, search, status, authorization);
        List<AdminUserAccessResponse> users = memberPage.items().stream()
                .map(member -> resolveUser(context, member))
                .toList();
        return PagedResult.<AdminUserAccessResponse>builder()
                .items(users)
                .totalElements(memberPage.totalElements())
                .build();
    }

    public PagedResult<AdminResourceAccessResponse> listResources(
            String organizationId,
            AdminResourceType resourceType,
            boolean includeInactiveUsers,
            int page,
            int size,
            String authorization,
            ActorResolver.Actor actor) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        MemberAccessResolver.ResolutionContext context = accessResolver.loadContext(organizationId);
        requireAdmin(context, organizationId, actor);
        List<ResourceRef> allResources = resources(context, resourceType);
        int from = Math.min(safePage * safeSize, allResources.size());
        int to = Math.min(from + safeSize, allResources.size());
        if (from == to) {
            return PagedResult.<AdminResourceAccessResponse>builder()
                    .items(List.of())
                    .totalElements(allResources.size())
                    .build();
        }

        List<ResolvedMember> members = fetchAllMembers(
                organizationId,
                includeInactiveUsers ? null : "ACTIVE",
                authorization).stream()
                .map(member -> new ResolvedMember(member, accessResolver.resolve(context, identity(organizationId, member))))
                .toList();
        List<AdminResourceAccessResponse> resources = allResources.subList(from, to).stream()
                .map(resource -> toResourceResponse(resource, members))
                .toList();
        return PagedResult.<AdminResourceAccessResponse>builder()
                .items(resources)
                .totalElements(allResources.size())
                .build();
    }

    public UserAccessBootstrapResponse bootstrap(
            String organizationId,
            String principalId,
            String authorization,
            ActorResolver.Actor actor) {
        MemberAccessResolver.ResolutionContext context = accessResolver.loadContext(organizationId);
        requireAdmin(context, organizationId, actor);
        OrganizationMemberRecord member = memberClient.findMember(organizationId, principalId, authorization)
                .orElseThrow(() -> new ResourceNotFoundException("Organization member not found: " + principalId));
        AdminUserAccessResponse access = resolveUser(context, member);
        return UserAccessBootstrapResponse.builder()
                .loginAccess(access)
                .tokenClaims(toTokenClaims(access))
                .build();
    }

    public AdminUserAccessResponse getUser(
            String organizationId,
            String principalId,
            String authorization,
            ActorResolver.Actor actor) {
        MemberAccessResolver.ResolutionContext context = accessResolver.loadContext(organizationId);
        requireAdmin(context, organizationId, actor);
        OrganizationMemberRecord member = memberClient.findMember(organizationId, principalId, authorization)
                .orElseThrow(() -> new ResourceNotFoundException("Organization member not found: " + principalId));
        return resolveUser(context, member);
    }

    public AdminResourceAccessResponse getResource(
            String organizationId,
            AdminResourceType resourceType,
            String resourceId,
            boolean includeInactiveUsers,
            String authorization,
            ActorResolver.Actor actor) {
        MemberAccessResolver.ResolutionContext context = accessResolver.loadContext(organizationId);
        requireAdmin(context, organizationId, actor);
        ResourceRef resource = resources(context, resourceType).stream()
                .filter(candidate -> candidate.id().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        resourceType + " not found: " + resourceId));
        List<ResolvedMember> members = fetchAllMembers(
                organizationId,
                includeInactiveUsers ? null : "ACTIVE",
                authorization).stream()
                .map(member -> new ResolvedMember(member, accessResolver.resolve(context, identity(organizationId, member))))
                .toList();
        return toResourceResponse(resource, members);
    }

    private AdminUserAccessResponse resolveUser(
            MemberAccessResolver.ResolutionContext context,
            OrganizationMemberRecord member) {
        MemberAccessResolver.Resolution resolution = accessResolver.resolve(
                context,
                identity(context.organizationId(), member));
        return AdminUserAccessResponse.builder()
                .member(toMemberResponse(context, member, resolution))
                .assignments(resolution.assignments())
                .effectiveAccess(toEffectiveAccess(context.organizationId(), member.email(), resolution))
                .build();
    }

    private AdminResourceAccessResponse toResourceResponse(
            ResourceRef resource,
            List<ResolvedMember> members) {
        List<ResourcePrincipalAccessResponse> users = members.stream()
                .map(member -> resourcePrincipal(resource, member))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ResourcePrincipalAccessResponse::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ResourcePrincipalAccessResponse::getEmail, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return AdminResourceAccessResponse.builder()
                .resourceType(resource.type())
                .resourceId(resource.id())
                .resourceName(resource.name())
                .status(resource.status())
                .businessUnitId(resource.businessUnitId())
                .projectId(resource.projectId())
                .userCount(users.size())
                .users(users)
                .build();
    }

    private ResourcePrincipalAccessResponse resourcePrincipal(ResourceRef resource, ResolvedMember member) {
        MemberAccessResolver.Resolution resolution = member.resolution();
        boolean canView = switch (resource.type()) {
            case BUSINESS_UNIT -> resolution.viewBusinessUnitIds().contains(resource.id());
            case PROJECT -> resolution.viewProjectIds().contains(resource.id());
            case APPLICATION -> resolution.viewApplicationIds().contains(resource.id());
        };
        boolean canManage = switch (resource.type()) {
            case BUSINESS_UNIT -> resolution.manageBusinessUnitIds().contains(resource.id());
            case PROJECT -> resolution.manageProjectIds().contains(resource.id());
            case APPLICATION -> resolution.manageApplicationIds().contains(resource.id());
        };
        boolean applicationMember = resource.type() == AdminResourceType.APPLICATION
                && resolution.memberApplicationIds().contains(resource.id());
        List<MemberRoleAssignmentResponse> relevant = resolution.assignments().stream()
                .filter(assignment -> contributesTo(resource, assignment))
                .toList();
        if (!canView && !canManage && !applicationMember && relevant.isEmpty()) return null;
        OrganizationMemberRecord identity = member.member();
        return ResourcePrincipalAccessResponse.builder()
                .principalId(identity.principalId())
                .email(identity.email())
                .name(identity.name())
                .organizationRole(identity.organizationRole())
                .canView(canView || canManage)
                .canManage(canManage)
                .applicationMember(applicationMember)
                .contributingAssignments(relevant)
                .build();
    }

    private boolean contributesTo(ResourceRef resource, MemberRoleAssignmentResponse assignment) {
        if (!assignment.isEffective()) return false;
        if ("ORGANIZATION".equals(assignment.getScopeType())) {
            return "ORG_ADMIN".equals(assignment.getRoleCode());
        }
        if (resource.type().name().equals(assignment.getScopeType())
                && resource.id().equals(assignment.getScopeId())) return true;
        if (resource.type() == AdminResourceType.PROJECT) {
            return "BUSINESS_UNIT".equals(assignment.getScopeType())
                    && Objects.equals(resource.businessUnitId(), assignment.getScopeId());
        }
        if (resource.type() == AdminResourceType.APPLICATION) {
            return ("BUSINESS_UNIT".equals(assignment.getScopeType())
                    && Objects.equals(resource.businessUnitId(), assignment.getScopeId()))
                    || ("PROJECT".equals(assignment.getScopeType())
                    && Objects.equals(resource.projectId(), assignment.getScopeId()));
        }
        return false;
    }

    private List<ResourceRef> resources(
            MemberAccessResolver.ResolutionContext context,
            AdminResourceType type) {
        List<ResourceRef> resources = new ArrayList<>();
        if (type == null || type == AdminResourceType.BUSINESS_UNIT) {
            context.businessUnits().forEach(unit -> resources.add(new ResourceRef(
                    AdminResourceType.BUSINESS_UNIT,
                    unit.getId(),
                    firstText(unit.getDisplayName(), unit.getName(), unit.getCode(), unit.getId()),
                    Objects.toString(unit.getStatus(), null),
                    unit.getId(),
                    null)));
        }
        if (type == null || type == AdminResourceType.PROJECT) {
            context.projects().forEach(project -> resources.add(new ResourceRef(
                    AdminResourceType.PROJECT,
                    project.getId(),
                    firstText(project.getName(), project.getCode(), project.getId()),
                    Objects.toString(project.getStatus(), null),
                    project.getBusinessUnitId(),
                    project.getId())));
        }
        if (type == null || type == AdminResourceType.APPLICATION) {
            context.applications().forEach(application -> resources.add(new ResourceRef(
                    AdminResourceType.APPLICATION,
                    application.getId(),
                    firstText(application.getDisplayName(), application.getName(), application.getApplicationId(), application.getId()),
                    Objects.toString(application.getStatus(), null),
                    application.getBusinessUnitId(),
                    application.getProjectId())));
        }
        return resources.stream()
                .sorted(Comparator.comparing((ResourceRef resource) -> resource.type().ordinal())
                        .thenComparing(ResourceRef::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<OrganizationMemberRecord> fetchAllMembers(
            String organizationId,
            String status,
            String authorization) {
        Map<String, OrganizationMemberRecord> members = new LinkedHashMap<>();
        for (int page = 0; page < MAX_MEMBER_PAGES; page++) {
            OrganizationMemberClient.MemberPage response = memberClient.fetchMembers(
                    organizationId, page, MEMBER_PAGE_SIZE, null, status, authorization);
            int before = members.size();
            response.items().forEach(member -> members.putIfAbsent(member.principalId(), member));
            if (response.items().isEmpty()
                    || members.size() >= response.totalElements()
                    || response.items().size() < MEMBER_PAGE_SIZE
                    || members.size() == before) break;
        }
        return List.copyOf(members.values());
    }

    private TokenAccessClaims toTokenClaims(AdminUserAccessResponse access) {
        EffectiveAccessResponse effective = access.getEffectiveAccess();
        Map<String, CompactRoleClaim> roles = access.getAssignments().stream()
                .filter(MemberRoleAssignmentResponse::isEffective)
                .filter(assignment -> (assignment.getPermissions() != null && !assignment.getPermissions().isEmpty())
                        || assignment.getRoleKind() == RoleKind.TOOL)
                .filter(assignment -> assignment.getRoleKind() != RoleKind.ORGANIZATION)
                .map(assignment -> CompactRoleClaim.builder()
                        .role(assignment.getRoleCode())
                        .scopeType(assignment.getScopeType())
                        .scopeId(assignment.getScopeId())
                        .permissions(assignment.getPermissions() == null ? List.of() : assignment.getPermissions())
                        .build())
                .collect(Collectors.toMap(
                        role -> String.join("|", role.getRole(), role.getScopeType(), role.getScopeId()),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        OrganizationMemberResponse member = access.getMember();
        return TokenAccessClaims.builder()
                .organizationId(member.getOrganizationId())
                .principalId(member.getPrincipalId())
                .email(member.getEmail())
                .role(member.getOrganizationRole())
                .orgAdmin(effective.isOrgAdmin())
                .viewBusinessUnitIds(sorted(effective.getViewBusinessUnitIds()))
                .manageBusinessUnitIds(sorted(effective.getManageBusinessUnitIds()))
                .viewProjectIds(sorted(effective.getViewProjectIds()))
                .manageProjectIds(sorted(effective.getManageProjectIds()))
                .viewApplicationIds(sorted(effective.getViewApplicationIds()))
                .manageApplicationIds(sorted(effective.getManageApplicationIds()))
                .memberApplicationIds(sorted(effective.getMemberApplicationIds()))
                .roleAssignments(List.copyOf(roles.values()))
                .accessGeneratedAt(Instant.now())
                .build();
    }

    private OrganizationMemberResponse toMemberResponse(
            MemberAccessResolver.ResolutionContext context,
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
                                assignment.getRoleKind(), assignment.getRoleCode(), assignment.getScopeType()),
                        LinkedHashMap::new,
                        Collectors.counting()));
        List<MemberRoleSummary> summaries = grouped.entrySet().stream()
                .map(entry -> MemberRoleSummary.builder()
                        .roleKind(entry.getKey().kind())
                        .roleCode(entry.getKey().code())
                        .scopeType(entry.getKey().scopeType())
                        .count(entry.getValue())
                        .build())
                .toList();
        return OrganizationMemberResponse.builder()
                .principalId(member.principalId())
                .organizationId(context.organizationId())
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
                .roles(summaries)
                .build();
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

    private void requireAdmin(
            MemberAccessResolver.ResolutionContext context,
            String organizationId,
            ActorResolver.Actor actor) {
        MemberAccessResolver.MemberIdentity caller = new MemberAccessResolver.MemberIdentity(
                StringUtils.hasText(actor.userId()) ? actor.userId() : actor.email(),
                organizationId,
                actor.email(),
                actor.name(),
                actor.role());
        if (!accessResolver.resolve(context, caller).orgAdmin()) {
            throw new io.probestack.onboarding.exception.ForbiddenOperationException(
                    "Only organization administrators can access the admin access catalog");
        }
    }

    private MemberAccessResolver.MemberIdentity identity(
            String organizationId,
            OrganizationMemberRecord member) {
        return new MemberAccessResolver.MemberIdentity(
                member.principalId(), organizationId, member.email(), member.name(), member.organizationRole());
    }

    private boolean emailEquals(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right)
                && left.trim().equalsIgnoreCase(right.trim());
    }

    private List<String> sorted(Collection<String> values) {
        return values == null ? List.of() : values.stream().sorted().toList();
    }

    private String firstText(String... values) {
        for (String value : values) if (StringUtils.hasText(value)) return value.trim();
        return "";
    }

    private record ResolvedMember(
            OrganizationMemberRecord member,
            MemberAccessResolver.Resolution resolution) {
    }

    private record ResourceRef(
            AdminResourceType type,
            String id,
            String name,
            String status,
            String businessUnitId,
            String projectId) {
    }

    private record RoleSummaryKey(RoleKind kind, String code, String scopeType) {
    }
}
