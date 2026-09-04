package io.probestack.onboarding;

import io.probestack.onboarding.dto.member.MemberRoleAssignmentResponse;
import io.probestack.onboarding.model.*;
import io.probestack.onboarding.repository.*;
import io.probestack.onboarding.service.MemberAccessResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MemberAccessResolverTest {
    private static final String ORG = "org-1";

    private final MemberAccessResolver resolver = new MemberAccessResolver(
            mock(PrincipalRoleAssignmentRepository.class),
            mock(AccessAssignmentRepository.class),
            mock(BusinessUnitRepository.class),
            mock(ProjectRepository.class),
            mock(ApplicationRepository.class),
            mock(ApplicationInvitationRepository.class),
            mock(AccessTeamRepository.class),
            mock(TeamInvitationRepository.class),
            mock(TeamApplicationGrantRepository.class),
            mock(DeveloperRepository.class),
            "");

    @Test
    void resolvesOwnershipDirectAssignmentsAndInheritedTeamAccessTogether() {
        BusinessUnit unit = BusinessUnit.builder().id("bu-1").organizationId(ORG).name("Payments").build();
        OnboardingProject project = OnboardingProject.builder()
                .id("project-1").organizationId(ORG).businessUnitId("bu-1").name("Checkout").build();
        OnboardingApplication application = OnboardingApplication.builder()
                .id("app-1").organizationId(ORG).businessUnitId("bu-1").projectId("project-1")
                .name("Gateway").ownerEmail("member@example.com").build();
        PrincipalRoleAssignment projectAdmin = PrincipalRoleAssignment.builder()
                .id("assignment-1").organizationId(ORG).principalId("user-1")
                .roleKind(RoleKind.ACCESS).roleCode("PROJECT_ADMIN")
                .scopeType("PROJECT").scopeId("project-1").active(true).build();
        TeamInvitation membership = TeamInvitation.builder()
                .id("membership-1").organizationId(ORG).teamId("team-1")
                .acceptedByEmail("member@example.com").status(InvitationStatus.ACCEPTED).build();
        TeamApplicationGrant teamGrant = TeamApplicationGrant.builder()
                .id("team-grant-1").organizationId(ORG).teamId("team-1").applicationId("app-1").build();
        AccessTeam team = AccessTeam.builder().id("team-1").organizationId(ORG).name("Platform Team").build();
        MemberAccessResolver.ResolutionContext context = new MemberAccessResolver.ResolutionContext(
                ORG, List.of(projectAdmin), List.of(), List.of(unit), List.of(project), List.of(application),
                List.of(), List.of(team), List.of(membership), List.of(teamGrant), List.of());

        MemberAccessResolver.Resolution result = resolver.resolve(context,
                new MemberAccessResolver.MemberIdentity("user-1", ORG, "member@example.com", "Member", "USER"));

        assertThat(result.manageProjectIds()).containsExactly("project-1");
        assertThat(result.manageApplicationIds()).containsExactly("app-1");
        assertThat(result.viewBusinessUnitIds()).containsExactly("bu-1");
        assertThat(result.memberApplicationIds()).containsExactly("app-1");
        assertThat(result.assignments())
                .extracting(MemberRoleAssignmentResponse::getSourceType)
                .contains(AssignmentSourceType.PRINCIPAL_ASSIGNMENT,
                        AssignmentSourceType.RESOURCE_OWNER,
                        AssignmentSourceType.TEAM_MEMBERSHIP,
                        AssignmentSourceType.TEAM_APPLICATION_GRANT);
    }

    @Test
    void orgAdminReceivesTheWholeOrganizationHierarchy() {
        BusinessUnit unit = BusinessUnit.builder().id("bu-1").organizationId(ORG).build();
        OnboardingProject project = OnboardingProject.builder().id("project-1").organizationId(ORG).businessUnitId("bu-1").build();
        OnboardingApplication app = OnboardingApplication.builder()
                .id("app-1").organizationId(ORG).businessUnitId("bu-1").projectId("project-1").build();
        MemberAccessResolver.ResolutionContext context = new MemberAccessResolver.ResolutionContext(
                ORG, List.of(), List.of(), List.of(unit), List.of(project), List.of(app),
                List.of(), List.of(), List.of(), List.of(), List.of());

        MemberAccessResolver.Resolution result = resolver.resolve(context,
                new MemberAccessResolver.MemberIdentity("admin-1", ORG, "admin@example.com", "Admin", "org_admin"));

        assertThat(result.orgAdmin()).isTrue();
        assertThat(result.manageBusinessUnitIds()).containsExactly("bu-1");
        assertThat(result.manageProjectIds()).containsExactly("project-1");
        assertThat(result.manageApplicationIds()).containsExactly("app-1");
    }
}
