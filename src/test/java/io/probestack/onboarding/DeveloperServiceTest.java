package io.probestack.onboarding;

import io.probestack.onboarding.dto.developer.DeveloperCreateRequest;
import io.probestack.onboarding.dto.developer.DeveloperUpdateRequest;
import io.probestack.onboarding.model.*;
import io.probestack.onboarding.repository.AccessTeamRepository;
import io.probestack.onboarding.repository.DeveloperRepository;
import io.probestack.onboarding.service.*;
import io.probestack.onboarding.util.ActorResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeveloperServiceTest {
    private static final String ORGANIZATION_ID = "org-001";
    private static final ActorResolver.Actor ACTOR = new ActorResolver.Actor("admin-1", "admin@example.com", "Admin", "ORG_ADMIN");

    @Mock private DeveloperRepository developerRepository;
    @Mock private BusinessUnitService businessUnitService;
    @Mock private ProjectService projectService;
    @Mock private ApplicationService applicationService;
    @Mock private AccessTeamRepository accessTeamRepository;
    @Mock private AuditService auditService;
    @Mock private AccessControlService accessControlService;

    @Test
    void create_normalizesIdentityAndMapsDeveloperSections() {
        when(businessUnitService.find(ORGANIZATION_ID, "bu-001")).thenReturn(BusinessUnit.builder().id("bu-001").organizationId(ORGANIZATION_ID).build());
        when(developerRepository.save(any())).thenAnswer(invocation -> {
            OnboardingDeveloper developer = invocation.getArgument(0);
            developer.setId("developer-001");
            return developer;
        });
        DeveloperService service = service();
        DeveloperCreateRequest request = new DeveloperCreateRequest();
        request.setBusinessUnitId("bu-001");
        request.setEmployeeId(" EMP-42 ");
        request.setEmail(" Developer@Example.COM ");
        request.setFirstName("Asha");
        request.setLastName("Rao");
        request.setUsername(" Asha.Rao ");
        request.setRole("Developer");
        request.setMfaEnabled(true);
        request.setGroups(List.of("engineering", " engineering ", "platform"));
        request.setPermissions(Map.of("api.read", true));
        request.setApiProvider(true);
        request.setModuleAccess(List.of("api_design", "MCP_INVENTORY"));
        request.setScopeGrants(List.of(DeveloperScopeGrant.builder()
                .levelCode("developer_l2").scopeType("ORGANIZATION").scopeId(ORGANIZATION_ID).build()));
        request.setQuotas(List.of(DeveloperQuota.builder()
                .quotaType("api_calls").quotaLimit(new BigDecimal("10000")).quotaUsed(new BigDecimal("9000")).build()));

        var response = service.create(ORGANIZATION_ID, request, ACTOR);

        assertEquals("developer@example.com", response.getEmail());
        assertEquals("asha.rao", response.getUsername());
        assertEquals("EMP-42", response.getEmployeeId());
        assertEquals(List.of("engineering", "platform"), response.getGroups());
        assertTrue(response.isMfaEnabled());
        assertTrue(response.isApiProvider());
        assertEquals(List.of("API_DESIGN", "MCP_INVENTORY"), response.getModuleAccess());
        assertEquals("DEVELOPER_L2", response.getScopeGrants().get(0).getLevelCode());
        assertEquals(BigDecimal.ZERO, response.getQuotas().get(0).getQuotaUsed());
    }

    @Test
    void update_preservesServerManagedQuotaUsage() {
        OnboardingDeveloper existing = OnboardingDeveloper.builder()
                .id("developer-001")
                .organizationId(ORGANIZATION_ID)
                .email("developer@example.com")
                .username("developer")
                .firstName("Asha")
                .lastName("Rao")
                .businessUnitId("bu-001")
                .role("Developer")
                .accountStatus(DeveloperAccountStatus.ACTIVE)
                .quotas(List.of(DeveloperQuota.builder()
                        .quotaType("api_calls").quotaLimit(new BigDecimal("1000")).quotaUsed(new BigDecimal("275")).build()))
                .build();
        when(developerRepository.findByIdAndOrganizationIdAndDeletedAtIsNull("developer-001", ORGANIZATION_ID)).thenReturn(Optional.of(existing));
        when(businessUnitService.find(ORGANIZATION_ID, "bu-001")).thenReturn(BusinessUnit.builder().id("bu-001").organizationId(ORGANIZATION_ID).build());
        when(developerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DeveloperUpdateRequest request = new DeveloperUpdateRequest();
        request.setQuotas(List.of(DeveloperQuota.builder()
                .quotaType("api_calls").quotaLimit(new BigDecimal("2000")).quotaUsed(new BigDecimal("999")).build()));

        var response = service().update(ORGANIZATION_ID, "developer-001", request, ACTOR);

        assertEquals(new BigDecimal("2000"), response.getQuotas().get(0).getQuotaLimit());
        assertEquals(new BigDecimal("275"), response.getQuotas().get(0).getQuotaUsed());
    }

    private DeveloperService service() {
        return new DeveloperService(developerRepository, businessUnitService, projectService, applicationService,
                accessTeamRepository, auditService, new PagingService(), accessControlService);
    }
}
