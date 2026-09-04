package io.probestack.onboarding;

import io.probestack.onboarding.client.OrganizationMemberClient;
import io.probestack.onboarding.client.OrganizationMemberRecord;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.dto.member.MemberRoleAssignmentResponse;
import io.probestack.onboarding.dto.member.OrganizationMemberResponse;
import io.probestack.onboarding.model.AssignmentSourceType;
import io.probestack.onboarding.model.OnboardingDeveloper;
import io.probestack.onboarding.model.RoleKind;
import io.probestack.onboarding.service.MemberAccessResolver;
import io.probestack.onboarding.service.OrganizationMemberService;
import io.probestack.onboarding.util.ActorResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationMemberServiceTest {
    private static final String ORG = "org-1";

    @Test
    void listsCanonicalActiveMembersAndEnrichesTheirOnboardingRoles() {
        OrganizationMemberClient memberClient = mock(OrganizationMemberClient.class);
        MemberAccessResolver resolver = mock(MemberAccessResolver.class);
        OrganizationMemberService service = new OrganizationMemberService(memberClient, resolver);
        OnboardingDeveloper profile = OnboardingDeveloper.builder()
                .id("developer-1").organizationId(ORG).email("member@example.com").role("API_ENGINEER").build();
        MemberAccessResolver.ResolutionContext context = new MemberAccessResolver.ResolutionContext(
                ORG, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(profile));
        ActorResolver.Actor caller = new ActorResolver.Actor("admin-1", "admin@example.com", "Admin", "ORG_ADMIN");
        OrganizationMemberRecord member = new OrganizationMemberRecord(
                "user-1", "member@example.com", "Member One", "USER", "ACTIVE", true);
        MemberAccessResolver.Resolution adminResolution = resolution(true, List.of());
        MemberRoleAssignmentResponse applicationOwner = MemberRoleAssignmentResponse.builder()
                .roleKind(RoleKind.RESPONSIBILITY).roleCode("APPLICATION_OWNER")
                .scopeType("APPLICATION").scopeId("app-1")
                .sourceType(AssignmentSourceType.RESOURCE_OWNER).active(true).effective(true).build();
        MemberAccessResolver.Resolution memberResolution = resolution(false, List.of(applicationOwner));
        when(resolver.loadContext(ORG)).thenReturn(context);
        when(resolver.resolve(eq(context), any())).thenAnswer(invocation -> {
            MemberAccessResolver.MemberIdentity identity = invocation.getArgument(1);
            return "admin-1".equals(identity.principalId()) ? adminResolution : memberResolution;
        });
        when(memberClient.fetchMembers(ORG, 0, 20, null, "ACTIVE", "Bearer token"))
                .thenReturn(new OrganizationMemberClient.MemberPage(List.of(member), 1));

        PagedResult<OrganizationMemberResponse> result = service.list(
                ORG, null, "ACTIVE", 0, 20, "Bearer token", caller);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getItems()).singleElement().satisfies(response -> {
            assertThat(response.getPrincipalId()).isEqualTo("user-1");
            assertThat(response.isDeveloperProfileConfigured()).isTrue();
            assertThat(response.getDeveloperRole()).isEqualTo("API_ENGINEER");
            assertThat(response.getAssignmentCount()).isEqualTo(1);
            assertThat(response.getRoles()).singleElement().satisfies(role ->
                    assertThat(role.getRoleCode()).isEqualTo("APPLICATION_OWNER"));
        });
        verify(memberClient).fetchMembers(ORG, 0, 20, null, "ACTIVE", "Bearer token");
    }

    private MemberAccessResolver.Resolution resolution(
            boolean orgAdmin,
            List<MemberRoleAssignmentResponse> assignments) {
        return new MemberAccessResolver.Resolution(assignments, orgAdmin,
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
    }
}
