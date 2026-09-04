package io.probestack.onboarding.service;

import io.probestack.onboarding.dto.member.MemberRoleAssignmentResponse;
import io.probestack.onboarding.model.*;
import io.probestack.onboarding.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MemberAccessResolver {
    private final PrincipalRoleAssignmentRepository principalAssignmentRepository;
    private final AccessAssignmentRepository legacyAssignmentRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final ProjectRepository projectRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationInvitationRepository applicationInvitationRepository;
    private final AccessTeamRepository teamRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamApplicationGrantRepository teamGrantRepository;
    private final DeveloperRepository developerRepository;
    private final Set<String> bootstrapOrgAdminEmails;

    public MemberAccessResolver(
            PrincipalRoleAssignmentRepository principalAssignmentRepository,
            AccessAssignmentRepository legacyAssignmentRepository,
            BusinessUnitRepository businessUnitRepository,
            ProjectRepository projectRepository,
            ApplicationRepository applicationRepository,
            ApplicationInvitationRepository applicationInvitationRepository,
            AccessTeamRepository teamRepository,
            TeamInvitationRepository teamInvitationRepository,
            TeamApplicationGrantRepository teamGrantRepository,
            DeveloperRepository developerRepository,
            @Value("${onboarding.access.org-admin-emails:}") String orgAdminEmails) {
        this.principalAssignmentRepository = principalAssignmentRepository;
        this.legacyAssignmentRepository = legacyAssignmentRepository;
        this.businessUnitRepository = businessUnitRepository;
        this.projectRepository = projectRepository;
        this.applicationRepository = applicationRepository;
        this.applicationInvitationRepository = applicationInvitationRepository;
        this.teamRepository = teamRepository;
        this.teamInvitationRepository = teamInvitationRepository;
        this.teamGrantRepository = teamGrantRepository;
        this.developerRepository = developerRepository;
        this.bootstrapOrgAdminEmails = Arrays.stream(orgAdminEmails.split(","))
                .map(this::normalizeEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    public ResolutionContext loadContext(String organizationId) {
        return new ResolutionContext(
                organizationId,
                principalAssignmentRepository.findByOrganizationIdAndActiveTrueOrderByCreatedAtDesc(organizationId),
                legacyAssignmentRepository.findByOrganizationIdAndActiveTrue(organizationId),
                businessUnitRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId),
                projectRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId),
                applicationRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId),
                applicationInvitationRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, InvitationStatus.ACCEPTED),
                teamRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId),
                teamInvitationRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                        .filter(invitation -> invitation.getStatus() == InvitationStatus.ACCEPTED)
                        .toList(),
                teamGrantRepository.findByOrganizationId(organizationId),
                developerRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId));
    }

    public Resolution resolve(ResolutionContext context, MemberIdentity member) {
        if (!context.organizationId().equals(member.organizationId())) {
            throw new IllegalArgumentException("Member organization does not match the resolver context");
        }
        Builder result = new Builder(context, member);
        addOrganizationRole(result, member);
        addPrincipalAssignments(result, context, member);
        addLegacyAssignments(result, context, member);
        addResourceResponsibilities(result, context, member);
        addApplicationInvitations(result, context, member);
        addTeamAssignments(result, context, member);
        addDeveloperProfileAssignments(result, context, member);
        result.expandParents();
        result.expandOrgAdmin();
        return result.build();
    }

    private void addOrganizationRole(Builder result, MemberIdentity member) {
        String role = normalizeCode(member.organizationRole(), "USER");
        result.add(null, RoleKind.ORGANIZATION, role, "ORGANIZATION", member.organizationId(),
                AssignmentSourceType.ADMIN_ACCOUNT, member.principalId(), null, null, null, List.of());
        if (bootstrapOrgAdminEmails.contains(normalizeEmail(member.email())) && !"ORG_ADMIN".equals(role)) {
            result.add(null, RoleKind.ORGANIZATION, "ORG_ADMIN", "ORGANIZATION", member.organizationId(),
                    AssignmentSourceType.BOOTSTRAP_CONFIG, member.email(), null, null, null, List.of("MANAGE"));
        }
    }

    private void addPrincipalAssignments(Builder result, ResolutionContext context, MemberIdentity member) {
        Instant now = Instant.now();
        context.principalAssignments().stream()
                .filter(assignment -> subjectMatches(member, assignment.getPrincipalId(), assignment.getPrincipalEmail()))
                .filter(assignment -> assignment.isActive()
                        && (assignment.getValidFrom() == null || !assignment.getValidFrom().isAfter(now))
                        && (assignment.getValidTo() == null || assignment.getValidTo().isAfter(now)))
                .forEach(assignment -> result.add(
                        assignment.getId(),
                        assignment.getRoleKind(),
                        assignment.getRoleCode(),
                        assignment.getScopeType(),
                        assignment.getScopeId(),
                        assignment.getSourceType(),
                        assignment.getSourceId(),
                        assignment.getInheritedFrom(),
                        assignment.getValidFrom(),
                        assignment.getValidTo(),
                        result.permissionsFor(assignment.getRoleCode(), assignment.getScopeType())));
    }

    private void addLegacyAssignments(Builder result, ResolutionContext context, MemberIdentity member) {
        context.legacyAssignments().stream()
                .filter(assignment -> emailEquals(member.email(), assignment.getPrincipalEmail()))
                .forEach(assignment -> result.add(
                        assignment.getId(),
                        assignment.getRole() == AccessRole.ORG_ADMIN ? RoleKind.ORGANIZATION : RoleKind.ACCESS,
                        assignment.getRole().name(),
                        assignment.getScopeType().name(),
                        assignment.getScopeId(),
                        AssignmentSourceType.LEGACY_ACCESS_ASSIGNMENT,
                        assignment.getId(),
                        null,
                        null,
                        null,
                        result.permissionsFor(assignment.getRole().name(), assignment.getScopeType().name())));
    }

    private void addResourceResponsibilities(Builder result, ResolutionContext context, MemberIdentity member) {
        for (BusinessUnit unit : context.businessUnits()) {
            if (emailEquals(member.email(), unit.getOwnerEmail())) {
                result.add(null, RoleKind.RESPONSIBILITY, "BUSINESS_UNIT_OWNER", "BUSINESS_UNIT", unit.getId(),
                        AssignmentSourceType.RESOURCE_OWNER, unit.getId(), null, null, null, List.of("MANAGE"));
            }
            addIdResponsibility(result, member, unit.getBusinessExecutiveId(), "BUSINESS_EXECUTIVE", "BUSINESS_UNIT", unit.getId());
            addIdResponsibility(result, member, unit.getBusinessOwnerId(), "BUSINESS_OWNER", "BUSINESS_UNIT", unit.getId());
            addIdResponsibility(result, member, unit.getProductOwnerId(), "PRODUCT_OWNER", "BUSINESS_UNIT", unit.getId());
            addIdResponsibility(result, member, unit.getTechnicalOwnerId(), "TECHNICAL_OWNER", "BUSINESS_UNIT", unit.getId());
            addIdResponsibility(result, member, unit.getEnterpriseArchitectId(), "ENTERPRISE_ARCHITECT", "BUSINESS_UNIT", unit.getId());
            addIdResponsibility(result, member, unit.getPlatformOwnerId(), "PLATFORM_OWNER", "BUSINESS_UNIT", unit.getId());
            addIdResponsibility(result, member, unit.getSecurityOwnerId(), "SECURITY_OWNER", "BUSINESS_UNIT", unit.getId());
            addIdResponsibility(result, member, unit.getComplianceOfficerId(), "COMPLIANCE_OFFICER", "BUSINESS_UNIT", unit.getId());
        }
        for (OnboardingProject project : context.projects()) {
            if (emailEquals(member.email(), project.getOwnerEmail())) {
                result.add(null, RoleKind.RESPONSIBILITY, "PROJECT_OWNER", "PROJECT", project.getId(),
                        AssignmentSourceType.RESOURCE_OWNER, project.getId(), null, null, null, List.of("MANAGE"));
            }
            addIdResponsibility(result, member, project.getProjectManagerId(), "PROJECT_MANAGER", "PROJECT", project.getId());
            addIdResponsibility(result, member, project.getProductManagerId(), "PRODUCT_MANAGER", "PROJECT", project.getId());
            addIdResponsibility(result, member, project.getScrumMasterId(), "SCRUM_MASTER", "PROJECT", project.getId());
            addIdResponsibility(result, member, project.getTechnicalLeadId(), "TECHNICAL_LEAD", "PROJECT", project.getId());
            addIdResponsibility(result, member, project.getSecurityLeadId(), "SECURITY_LEAD", "PROJECT", project.getId());
            addIdResponsibility(result, member, project.getDevopsLeadId(), "DEVOPS_LEAD", "PROJECT", project.getId());
        }
        for (OnboardingApplication application : context.applications()) {
            if (emailEquals(member.email(), application.getOwnerEmail())) {
                result.add(null, RoleKind.RESPONSIBILITY, "APPLICATION_OWNER", "APPLICATION", application.getId(),
                        AssignmentSourceType.RESOURCE_OWNER, application.getId(), null, null, null, List.of("MANAGE"));
            }
            addEmailResponsibility(result, member, application.getSmeEmail(), "APPLICATION_SME", "APPLICATION", application.getId());
            addEmailResponsibility(result, member, application.getTesterEmail(), "APPLICATION_TESTER", "APPLICATION", application.getId());
            addEmailResponsibility(result, member, application.getServiceNowEmail(), "SERVICE_NOW_CONTACT", "APPLICATION", application.getId());
        }
    }

    private void addApplicationInvitations(Builder result, ResolutionContext context, MemberIdentity member) {
        context.applicationInvitations().stream()
                .filter(invitation -> emailEquals(member.email(), invitation.getAcceptedByEmail())
                        || (!StringUtils.hasText(invitation.getAcceptedByEmail()) && emailEquals(member.email(), invitation.getInvitedEmail())))
                .forEach(invitation -> {
                    String role = invitation.getRole() == null ? AccessRole.APPLICATION_MEMBER.name() : invitation.getRole().name();
                    result.add(invitation.getId(), RoleKind.ACCESS, role, "APPLICATION", invitation.getApplicationId(),
                            AssignmentSourceType.APPLICATION_INVITATION, invitation.getId(), null, null, null,
                            result.permissionsFor(role, "APPLICATION"));
                    addToolGrants(result, invitation.getToolRoleGrants(), "APPLICATION", invitation.getApplicationId(),
                            AssignmentSourceType.APPLICATION_INVITATION, invitation.getId(), null);
                });
    }

    private void addTeamAssignments(Builder result, ResolutionContext context, MemberIdentity member) {
        Map<String, AccessTeam> teamsById = context.teams().stream()
                .collect(Collectors.toMap(AccessTeam::getId, team -> team, (left, right) -> left));
        Map<String, List<TeamApplicationGrant>> grantsByTeam = context.teamApplicationGrants().stream()
                .collect(Collectors.groupingBy(TeamApplicationGrant::getTeamId));
        context.teamInvitations().stream()
                .filter(invitation -> emailEquals(member.email(), invitation.getAcceptedByEmail())
                        || (!StringUtils.hasText(invitation.getAcceptedByEmail()) && emailEquals(member.email(), invitation.getInvitedEmail())))
                .forEach(invitation -> {
                    AccessTeam team = teamsById.get(invitation.getTeamId());
                    result.add(invitation.getId(), RoleKind.RESPONSIBILITY, "TEAM_MEMBER", "TEAM", invitation.getTeamId(),
                            AssignmentSourceType.TEAM_MEMBERSHIP, invitation.getId(), null, null, null, List.of());
                    addToolGrants(result, invitation.getToolRoleGrants(), "TEAM", invitation.getTeamId(),
                            AssignmentSourceType.TEAM_MEMBERSHIP, invitation.getId(), null);
                    for (TeamApplicationGrant grant : grantsByTeam.getOrDefault(invitation.getTeamId(), List.of())) {
                        result.add(grant.getId(), RoleKind.ACCESS, AccessRole.APPLICATION_MEMBER.name(), "APPLICATION", grant.getApplicationId(),
                                AssignmentSourceType.TEAM_APPLICATION_GRANT, grant.getId(),
                                team == null ? invitation.getTeamId() : team.getId(), null, null, List.of("VIEW", "MEMBER"));
                    }
                });
    }

    private void addDeveloperProfileAssignments(Builder result, ResolutionContext context, MemberIdentity member) {
        context.developers().stream()
                .filter(developer -> emailEquals(member.email(), developer.getEmail()))
                .forEach(developer -> {
                    if (developer.getScopeGrants() != null) {
                        for (DeveloperScopeGrant grant : developer.getScopeGrants()) {
                            String roleCode = normalizeCode(grant.getLevelCode(), "UNSPECIFIED");
                            String scopeType = normalizeCode(grant.getScopeType(), "ORGANIZATION");
                            result.add(null, RoleKind.ACCESS, roleCode, scopeType, grant.getScopeId(),
                                    AssignmentSourceType.DEVELOPER_SCOPE_GRANT, developer.getId(), null, null, null,
                                    result.permissionsFor(roleCode, scopeType));
                        }
                    }
                    addProfileTool(result, developer, developer.isApiConsumer(), "API_CONSUMER");
                    addProfileTool(result, developer, developer.isApiProvider(), "API_PROVIDER");
                    addProfileTool(result, developer, developer.isAiEngineer(), "AI_ENGINEER");
                    addProfileTool(result, developer, developer.isGatewayAdmin(), "GATEWAY_ADMIN");
                    if (developer.getModuleAccess() != null) {
                        for (String module : developer.getModuleAccess()) {
                            result.add(null, RoleKind.TOOL, normalizeCode(module, "MODULE_ACCESS"), "TOOL", normalizeCode(module, "MODULE"),
                                    AssignmentSourceType.DEVELOPER_PROFILE, developer.getId(), null, null, null, List.of());
                        }
                    }
                });
    }

    private void addProfileTool(Builder result, OnboardingDeveloper developer, boolean enabled, String roleCode) {
        if (!enabled) return;
        result.add(null, RoleKind.TOOL, roleCode, "ORGANIZATION", developer.getOrganizationId(),
                AssignmentSourceType.DEVELOPER_PROFILE, developer.getId(), null, null, null, List.of());
    }

    private void addToolGrants(
            Builder result,
            List<ToolRoleGrant> grants,
            String scopeType,
            String scopeId,
            AssignmentSourceType sourceType,
            String sourceId,
            String inheritedFrom) {
        if (grants == null) return;
        for (ToolRoleGrant grant : grants) {
            result.add(null, RoleKind.TOOL, normalizeCode(grant.getRole(), "USER"), scopeType, scopeId,
                    sourceType, sourceId, inheritedFrom, null, null, List.of());
        }
    }

    private void addIdResponsibility(
            Builder result,
            MemberIdentity member,
            String assignedPrincipalId,
            String roleCode,
            String scopeType,
            String scopeId) {
        if (StringUtils.hasText(assignedPrincipalId) && assignedPrincipalId.trim().equals(member.principalId())) {
            result.add(null, RoleKind.RESPONSIBILITY, roleCode, scopeType, scopeId,
                    AssignmentSourceType.RESOURCE_OWNER, scopeId, null, null, null, List.of());
        }
    }

    private void addEmailResponsibility(
            Builder result,
            MemberIdentity member,
            String assignedEmail,
            String roleCode,
            String scopeType,
            String scopeId) {
        if (emailEquals(member.email(), assignedEmail)) {
            result.add(null, RoleKind.RESPONSIBILITY, roleCode, scopeType, scopeId,
                    AssignmentSourceType.RESOURCE_OWNER, scopeId, null, null, null, List.of());
        }
    }

    private boolean subjectMatches(MemberIdentity member, String principalId, String email) {
        return (StringUtils.hasText(principalId) && principalId.trim().equals(member.principalId()))
                || emailEquals(member.email(), email);
    }

    private boolean emailEquals(String left, String right) {
        String normalizedLeft = normalizeEmail(left);
        return normalizedLeft != null && normalizedLeft.equals(normalizeEmail(right));
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    private static String normalizeCode(String value, String fallback) {
        return StringUtils.hasText(value)
                ? value.trim().replaceAll("[-\\s]+", "_").toUpperCase(Locale.ROOT)
                : fallback;
    }

    public record MemberIdentity(
            String principalId,
            String organizationId,
            String email,
            String name,
            String organizationRole) {
    }

    public record ResolutionContext(
            String organizationId,
            List<PrincipalRoleAssignment> principalAssignments,
            List<AccessAssignment> legacyAssignments,
            List<BusinessUnit> businessUnits,
            List<OnboardingProject> projects,
            List<OnboardingApplication> applications,
            List<ApplicationInvitation> applicationInvitations,
            List<AccessTeam> teams,
            List<TeamInvitation> teamInvitations,
            List<TeamApplicationGrant> teamApplicationGrants,
            List<OnboardingDeveloper> developers) {
    }

    public record Resolution(
            List<MemberRoleAssignmentResponse> assignments,
            boolean orgAdmin,
            Set<String> viewBusinessUnitIds,
            Set<String> manageBusinessUnitIds,
            Set<String> viewProjectIds,
            Set<String> manageProjectIds,
            Set<String> viewApplicationIds,
            Set<String> manageApplicationIds,
            Set<String> memberApplicationIds) {
    }

    private static final class Builder {
        private final ResolutionContext context;
        private final MemberIdentity member;
        private final List<MemberRoleAssignmentResponse> assignments = new ArrayList<>();
        private final Set<String> contributionKeys = new HashSet<>();
        private boolean orgAdmin;
        private final Set<String> viewBusinessUnitIds = new LinkedHashSet<>();
        private final Set<String> manageBusinessUnitIds = new LinkedHashSet<>();
        private final Set<String> viewProjectIds = new LinkedHashSet<>();
        private final Set<String> manageProjectIds = new LinkedHashSet<>();
        private final Set<String> viewApplicationIds = new LinkedHashSet<>();
        private final Set<String> manageApplicationIds = new LinkedHashSet<>();
        private final Set<String> memberApplicationIds = new LinkedHashSet<>();
        private final Map<String, String> scopeNames = new HashMap<>();

        private Builder(ResolutionContext context, MemberIdentity member) {
            this.context = context;
            this.member = member;
            scopeNames.put(context.organizationId(), context.organizationId());
            context.businessUnits().forEach(unit -> scopeNames.put(unit.getId(), firstText(unit.getDisplayName(), unit.getName(), unit.getCode(), unit.getId())));
            context.projects().forEach(project -> scopeNames.put(project.getId(), firstText(project.getName(), project.getCode(), project.getId())));
            context.applications().forEach(app -> scopeNames.put(app.getId(), firstText(app.getDisplayName(), app.getName(), app.getApplicationId(), app.getId())));
            context.teams().forEach(team -> scopeNames.put(team.getId(), firstText(team.getName(), team.getId())));
        }

        private void add(
                String id,
                RoleKind roleKind,
                String roleCode,
                String scopeType,
                String scopeId,
                AssignmentSourceType sourceType,
                String sourceId,
                String inheritedFrom,
                Instant validFrom,
                Instant validTo,
                List<String> permissions) {
            RoleKind kind = roleKind == null ? RoleKind.ACCESS : roleKind;
            String code = normalizeCode(roleCode, "UNSPECIFIED");
            String normalizedScope = normalizeCode(scopeType, "ORGANIZATION");
            String resolvedScopeId = StringUtils.hasText(scopeId) ? scopeId : context.organizationId();
            AssignmentSourceType resolvedSource = sourceType == null ? AssignmentSourceType.PRINCIPAL_ASSIGNMENT : sourceType;
            String key = String.join("|", kind.name(), code, normalizedScope, resolvedScopeId,
                    resolvedSource.name(), Objects.toString(sourceId, ""), Objects.toString(inheritedFrom, ""));
            if (!contributionKeys.add(key)) return;
            List<String> normalizedPermissions = permissions == null ? List.of() : List.copyOf(new LinkedHashSet<>(permissions));
            assignments.add(MemberRoleAssignmentResponse.builder()
                    .id(id)
                    .roleKind(kind)
                    .roleCode(code)
                    .scopeType(normalizedScope)
                    .scopeId(resolvedScopeId)
                    .scopeName(scopeNames.getOrDefault(resolvedScopeId, resolvedScopeId))
                    .sourceType(resolvedSource)
                    .sourceId(sourceId)
                    .inheritedFrom(inheritedFrom)
                    .active(true)
                    .effective(true)
                    .permissions(normalizedPermissions)
                    .validFrom(validFrom)
                    .validTo(validTo)
                    .build());
            applyPermission(code, normalizedScope, resolvedScopeId);
        }

        private List<String> permissionsFor(String roleCode, String scopeType) {
            String code = normalizeCode(roleCode, "");
            String scope = normalizeCode(scopeType, "");
            if ("ORG_ADMIN".equals(code) && "ORGANIZATION".equals(scope)) return List.of("MANAGE");
            if (Set.of("BUSINESS_UNIT_ADMIN", "BUSINESS_UNIT_OWNER").contains(code) && "BUSINESS_UNIT".equals(scope)) return List.of("MANAGE");
            if (Set.of("PROJECT_ADMIN", "PROJECT_OWNER").contains(code) && "PROJECT".equals(scope)) return List.of("MANAGE");
            if ("APPLICATION_OWNER".equals(code) && "APPLICATION".equals(scope)) return List.of("MANAGE");
            if ("APPLICATION_MEMBER".equals(code) && "APPLICATION".equals(scope)) return List.of("VIEW", "MEMBER");
            return List.of();
        }

        private void applyPermission(String roleCode, String scopeType, String scopeId) {
            if ("ORG_ADMIN".equals(roleCode) && "ORGANIZATION".equals(scopeType)) {
                orgAdmin = true;
            } else if (Set.of("BUSINESS_UNIT_ADMIN", "BUSINESS_UNIT_OWNER").contains(roleCode) && "BUSINESS_UNIT".equals(scopeType)) {
                addBusinessUnitAdmin(scopeId);
            } else if (Set.of("PROJECT_ADMIN", "PROJECT_OWNER").contains(roleCode) && "PROJECT".equals(scopeType)) {
                addProjectAdmin(scopeId);
            } else if ("APPLICATION_OWNER".equals(roleCode) && "APPLICATION".equals(scopeType)) {
                addApplicationOwner(scopeId);
            } else if ("APPLICATION_MEMBER".equals(roleCode) && "APPLICATION".equals(scopeType)) {
                viewApplicationIds.add(scopeId);
                memberApplicationIds.add(scopeId);
            }
        }

        private void addBusinessUnitAdmin(String businessUnitId) {
            manageBusinessUnitIds.add(businessUnitId);
            viewBusinessUnitIds.add(businessUnitId);
            context.projects().stream()
                    .filter(project -> businessUnitId.equals(project.getBusinessUnitId()))
                    .forEach(project -> addProjectAdmin(project.getId()));
        }

        private void addProjectAdmin(String projectId) {
            manageProjectIds.add(projectId);
            viewProjectIds.add(projectId);
            context.applications().stream()
                    .filter(app -> projectId.equals(app.getProjectId()))
                    .forEach(app -> addApplicationOwner(app.getId()));
        }

        private void addApplicationOwner(String applicationId) {
            manageApplicationIds.add(applicationId);
            viewApplicationIds.add(applicationId);
        }

        private void expandParents() {
            for (OnboardingApplication app : context.applications()) {
                if (viewApplicationIds.contains(app.getId()) || manageApplicationIds.contains(app.getId())) {
                    if (StringUtils.hasText(app.getProjectId())) viewProjectIds.add(app.getProjectId());
                    if (StringUtils.hasText(app.getBusinessUnitId())) viewBusinessUnitIds.add(app.getBusinessUnitId());
                }
            }
            for (OnboardingProject project : context.projects()) {
                if (viewProjectIds.contains(project.getId()) || manageProjectIds.contains(project.getId())) {
                    if (StringUtils.hasText(project.getBusinessUnitId())) viewBusinessUnitIds.add(project.getBusinessUnitId());
                }
            }
        }

        private void expandOrgAdmin() {
            if (!orgAdmin) return;
            context.businessUnits().forEach(unit -> {
                manageBusinessUnitIds.add(unit.getId());
                viewBusinessUnitIds.add(unit.getId());
            });
            context.projects().forEach(project -> {
                manageProjectIds.add(project.getId());
                viewProjectIds.add(project.getId());
            });
            context.applications().forEach(app -> {
                manageApplicationIds.add(app.getId());
                viewApplicationIds.add(app.getId());
            });
        }

        private Resolution build() {
            assignments.sort(Comparator
                    .comparing((MemberRoleAssignmentResponse assignment) -> assignment.getRoleKind().ordinal())
                    .thenComparing(MemberRoleAssignmentResponse::getRoleCode)
                    .thenComparing(MemberRoleAssignmentResponse::getScopeType)
                    .thenComparing(MemberRoleAssignmentResponse::getScopeName));
            return new Resolution(
                    List.copyOf(assignments),
                    orgAdmin,
                    Set.copyOf(viewBusinessUnitIds),
                    Set.copyOf(manageBusinessUnitIds),
                    Set.copyOf(viewProjectIds),
                    Set.copyOf(manageProjectIds),
                    Set.copyOf(viewApplicationIds),
                    Set.copyOf(manageApplicationIds),
                    Set.copyOf(memberApplicationIds));
        }

        private static String firstText(String... values) {
            for (String value : values) {
                if (StringUtils.hasText(value)) return value.trim();
            }
            return "";
        }
    }
}
