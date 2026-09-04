package io.probestack.onboarding;

import io.probestack.onboarding.client.OrganizationMemberClient;
import io.probestack.onboarding.client.OrganizationMemberRecord;
import io.probestack.onboarding.dto.member.MemberRoleAssignmentResponse;
import io.probestack.onboarding.dto.member.RoleAssignmentCreateRequest;
import io.probestack.onboarding.model.PrincipalRoleAssignment;
import io.probestack.onboarding.model.RoleKind;
import io.probestack.onboarding.repository.PrincipalRoleAssignmentRepository;
import io.probestack.onboarding.service.AccessControlService;
import io.probestack.onboarding.service.AuditService;
import io.probestack.onboarding.service.PrincipalRoleAssignmentService;
import io.probestack.onboarding.util.ActorResolver;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrincipalRoleAssignmentServiceTest {
    @Test
    void createsAssignmentWithCanonicalMemberIdentity() {
        PrincipalRoleAssignmentRepository repository = mock(PrincipalRoleAssignmentRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        AuditService auditService = mock(AuditService.class);
        OrganizationMemberClient memberClient = mock(OrganizationMemberClient.class);
        PrincipalRoleAssignmentService service = new PrincipalRoleAssignmentService(
                repository, accessControl, auditService, memberClient);
        ActorResolver.Actor actor = new ActorResolver.Actor("admin-1", "admin@example.com", "Admin", "ORG_ADMIN");
        when(memberClient.findMember("org-1", "user-1", "Bearer token")).thenReturn(Optional.of(
                new OrganizationMemberRecord("user-1", "Canonical@Example.com", "Canonical Name", "USER", "ACTIVE", true)));
        when(repository.save(any())).thenAnswer(invocation -> {
            PrincipalRoleAssignment value = invocation.getArgument(0);
            value.setId("assignment-1");
            return value;
        });
        RoleAssignmentCreateRequest request = new RoleAssignmentCreateRequest();
        request.setPrincipalId("user-1");
        request.setPrincipalEmail("spoofed@example.com");
        request.setPrincipalName("Spoofed Name");
        request.setRoleKind(RoleKind.ACCESS);
        request.setRoleCode("project-admin");
        request.setScopeType("PROJECT");
        request.setScopeId("project-1");

        MemberRoleAssignmentResponse result = service.create("org-1", request, "Bearer token", actor);

        assertThat(result.getId()).isEqualTo("assignment-1");
        assertThat(result.getRoleCode()).isEqualTo("PROJECT_ADMIN");
        verify(accessControl).requireOrgAdmin("org-1", actor);
        verify(repository).save(org.mockito.ArgumentMatchers.argThat(assignment ->
                "canonical@example.com".equals(assignment.getPrincipalEmail())
                        && "Canonical Name".equals(assignment.getPrincipalName())));
    }
}
