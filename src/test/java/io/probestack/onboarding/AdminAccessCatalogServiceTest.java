package io.probestack.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.probestack.onboarding.client.OrganizationMemberClient;
import io.probestack.onboarding.client.OrganizationMemberRecord;
import io.probestack.onboarding.dto.adminaccess.AdminResourceAccessResponse;
import io.probestack.onboarding.dto.adminaccess.AdminResourceType;
import io.probestack.onboarding.dto.adminaccess.UserAccessBootstrapResponse;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.dto.member.MemberRoleAssignmentResponse;
import io.probestack.onboarding.exception.ForbiddenOperationException;
import io.probestack.onboarding.model.*;
import io.probestack.onboarding.service.AdminAccessCatalogService;
import io.probestack.onboarding.service.MemberAccessResolver;
import io.probestack.onboarding.util.ActorResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAccessCatalogServiceTest {
    private static final String ORG = "org-1";
    private static final ActorResolver.Actor ADMIN =
            new ActorResolver.Actor("admin-1", "admin@example.com", "Admin", "ORG_ADMIN");

    @Test
    void buildsResourceCentricAccessWithInheritedProjectRole() {
        OrganizationMemberClient memberClient = mock(OrganizationMemberClient.class);
        MemberAccessResolver resolver = mock(MemberAccessResolver.class);
        AdminAccessCatalogService service = new AdminAccessCatalogService(memberClient, resolver);
        MemberAccessResolver.ResolutionContext context = context();
        OrganizationMemberRecord member = member();
        MemberRoleAssignmentResponse projectAdmin = projectAdminAssignment();
        MemberAccessResolver.Resolution memberResolution = new MemberAccessResolver.Resolution(
                List.of(projectAdmin), false,
                Set.of("bu-1"), Set.of(),
                Set.of("project-1"), Set.of("project-1"),
                Set.of("app-1"), Set.of("app-1"), Set.of());
        when(resolver.loadContext(ORG)).thenReturn(context);
        when(resolver.resolve(eq(context), any())).thenAnswer(invocation -> {
            MemberAccessResolver.MemberIdentity identity = invocation.getArgument(1);
            return "admin-1".equals(identity.principalId()) ? adminResolution() : memberResolution;
        });
        when(memberClient.fetchMembers(ORG, 0, 200, null, "ACTIVE", "Bearer token"))
                .thenReturn(new OrganizationMemberClient.MemberPage(List.of(member), 1));

        PagedResult<AdminResourceAccessResponse> result = service.listResources(
                ORG, null, false, 0, 20, "Bearer token", ADMIN);

        assertThat(result.getTotalElements()).isEqualTo(3);
        AdminResourceAccessResponse application = result.getItems().stream()
                .filter(resource -> resource.getResourceType() == AdminResourceType.APPLICATION)
                .findFirst()
                .orElseThrow();
        assertThat(application.getUsers()).singleElement().satisfies(access -> {
            assertThat(access.isCanView()).isTrue();
            assertThat(access.isCanManage()).isTrue();
            assertThat(access.getContributingAssignments()).containsExactly(projectAdmin);
        });
    }

    @Test
    void returnsFullLoginAccessAndCompactSnakeCaseTokenClaims() throws Exception {
        OrganizationMemberClient memberClient = mock(OrganizationMemberClient.class);
        MemberAccessResolver resolver = mock(MemberAccessResolver.class);
        AdminAccessCatalogService service = new AdminAccessCatalogService(memberClient, resolver);
        MemberAccessResolver.ResolutionContext context = context();
        OrganizationMemberRecord member = member();
        MemberAccessResolver.Resolution memberResolution = new MemberAccessResolver.Resolution(
                List.of(projectAdminAssignment()), false,
                Set.of("bu-1"), Set.of(),
                Set.of("project-1"), Set.of("project-1"),
                Set.of("app-1"), Set.of("app-1"), Set.of());
        when(resolver.loadContext(ORG)).thenReturn(context);
        when(resolver.resolve(eq(context), any())).thenAnswer(invocation -> {
            MemberAccessResolver.MemberIdentity identity = invocation.getArgument(1);
            return "admin-1".equals(identity.principalId()) ? adminResolution() : memberResolution;
        });
        when(memberClient.findMember(ORG, "user-1", "Bearer token")).thenReturn(Optional.of(member));

        UserAccessBootstrapResponse result = service.bootstrap(ORG, "user-1", "Bearer token", ADMIN);
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(result.getTokenClaims());

        assertThat(result.getLoginAccess().getAssignments()).hasSize(1);
        assertThat(result.getTokenClaims().getManageProjectIds()).containsExactly("project-1");
        assertThat(result.getTokenClaims().getManageApplicationIds()).containsExactly("app-1");
        assertThat(result.getTokenClaims().getRoleAssignments()).singleElement()
                .satisfies(role -> assertThat(role.getRole()).isEqualTo("PROJECT_ADMIN"));
        assertThat(json).contains("\"organization_id\":\"org-1\"")
                .contains("\"manage_application_ids\":[\"app-1\"]")
                .contains("\"role_assignments\"");
    }

    @Test
    void rejectsNonAdminCallersBeforeReadingCanonicalMembers() {
        OrganizationMemberClient memberClient = mock(OrganizationMemberClient.class);
        MemberAccessResolver resolver = mock(MemberAccessResolver.class);
        AdminAccessCatalogService service = new AdminAccessCatalogService(memberClient, resolver);
        MemberAccessResolver.ResolutionContext context = context();
        when(resolver.loadContext(ORG)).thenReturn(context);
        when(resolver.resolve(eq(context), any())).thenReturn(new MemberAccessResolver.Resolution(
                List.of(), false, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of()));

        assertThatThrownBy(() -> service.listUsers(
                ORG, null, "ACTIVE", 0, 20, "Bearer token",
                new ActorResolver.Actor("user-2", "user@example.com", "User", "USER")))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private MemberAccessResolver.ResolutionContext context() {
        BusinessUnit unit = BusinessUnit.builder()
                .id("bu-1").organizationId(ORG).name("Payments").status(BusinessUnitStatus.ACTIVE).build();
        OnboardingProject project = OnboardingProject.builder()
                .id("project-1").organizationId(ORG).businessUnitId("bu-1")
                .name("Checkout").status(ProjectStatus.READY).build();
        OnboardingApplication application = OnboardingApplication.builder()
                .id("app-1").organizationId(ORG).businessUnitId("bu-1").projectId("project-1")
                .name("Gateway").status(ApplicationStatus.ACTIVE).build();
        return new MemberAccessResolver.ResolutionContext(
                ORG, List.of(), List.of(), List.of(unit), List.of(project), List.of(application),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private OrganizationMemberRecord member() {
        return new OrganizationMemberRecord(
                "user-1", "member@example.com", "Member One", "USER", "ACTIVE", true);
    }

    private MemberRoleAssignmentResponse projectAdminAssignment() {
        return MemberRoleAssignmentResponse.builder()
                .id("assignment-1")
                .roleKind(RoleKind.ACCESS)
                .roleCode("PROJECT_ADMIN")
                .scopeType("PROJECT")
                .scopeId("project-1")
                .scopeName("Checkout")
                .sourceType(AssignmentSourceType.PRINCIPAL_ASSIGNMENT)
                .active(true)
                .effective(true)
                .permissions(List.of("MANAGE"))
                .build();
    }

    private MemberAccessResolver.Resolution adminResolution() {
        return new MemberAccessResolver.Resolution(
                List.of(), true, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
    }
}
