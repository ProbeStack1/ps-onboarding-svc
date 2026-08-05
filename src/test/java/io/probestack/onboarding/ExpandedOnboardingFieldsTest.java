package io.probestack.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.probestack.onboarding.dto.application.ApplicationCreateRequest;
import io.probestack.onboarding.dto.businessunit.BusinessUnitCreateRequest;
import io.probestack.onboarding.dto.businessunit.BusinessUnitUpdateRequest;
import io.probestack.onboarding.dto.project.ProjectCreateRequest;
import io.probestack.onboarding.model.BusinessUnit;
import io.probestack.onboarding.model.BusinessUnitQuota;
import io.probestack.onboarding.model.BusinessUnitStatus;
import io.probestack.onboarding.model.OnboardingApplication;
import io.probestack.onboarding.model.OnboardingProject;
import io.probestack.onboarding.model.ProjectEnvironment;
import io.probestack.onboarding.repository.ApplicationConsumerLinkRepository;
import io.probestack.onboarding.repository.ApplicationRepository;
import io.probestack.onboarding.repository.BusinessUnitRepository;
import io.probestack.onboarding.repository.ConsumerRepository;
import io.probestack.onboarding.repository.ProjectRepository;
import io.probestack.onboarding.service.AccessControlService;
import io.probestack.onboarding.service.ApplicationService;
import io.probestack.onboarding.service.AuditService;
import io.probestack.onboarding.service.BusinessUnitService;
import io.probestack.onboarding.service.PagingService;
import io.probestack.onboarding.service.ProjectService;
import io.probestack.onboarding.util.ActorResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpandedOnboardingFieldsTest {
    private static final String ORGANIZATION_ID = "org-001";
    private static final ActorResolver.Actor ACTOR = new ActorResolver.Actor("user-1", "admin@example.com", "Admin", "ADMIN");

    @Mock private BusinessUnitRepository businessUnitRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationConsumerLinkRepository linkRepository;
    @Mock private ConsumerRepository consumerRepository;
    @Mock private AuditService auditService;
    @Mock private AccessControlService accessControlService;
    @Mock private BusinessUnitService businessUnitService;
    @Mock private ProjectService projectService;

    @Test
    void businessUnitCreate_mapsExpandedFieldsAndOwnsQuotaUsage() {
        BusinessUnitService service = new BusinessUnitService(businessUnitRepository, projectRepository,
                applicationRepository, linkRepository, auditService, new PagingService(), accessControlService);
        when(businessUnitRepository.save(any())).thenAnswer(invocation -> {
            BusinessUnit value = invocation.getArgument(0);
            value.setId("bu-001");
            return value;
        });

        BusinessUnitCreateRequest request = new BusinessUnitCreateRequest();
        request.setName("Digital Banking");
        request.setCode("digital-banking");
        request.setDisplayName("Digital Banking BU");
        request.setBusinessExecutiveId(" executive-1 ");
        request.setMonthlyBudget(new BigDecimal("250000.50"));
        request.setCloudProvider("AWS");
        request.setRiskClassification("High");
        request.setRegulatoryStandards(List.of("PCI-DSS", " PCI-DSS ", "SOC 2"));
        request.setDrEnabled(true);
        request.setQuotas(List.of(BusinessUnitQuota.builder()
                .quotaType("api_calls")
                .quotaLimit(new BigDecimal("1000000"))
                .quotaUsed(new BigDecimal("999"))
                .build()));

        var response = service.create(ORGANIZATION_ID, request, ACTOR);

        assertEquals("DIGITAL-BANKING", response.getCode());
        assertEquals("Digital Banking BU", response.getDisplayName());
        assertEquals("executive-1", response.getBusinessExecutiveId());
        assertEquals(new BigDecimal("250000.50"), response.getMonthlyBudget());
        assertEquals(List.of("PCI-DSS", "SOC 2"), response.getRegulatoryStandards());
        assertTrue(response.isDrEnabled());
        assertEquals(BigDecimal.ZERO, response.getQuotas().get(0).getQuotaUsed());
    }

    @Test
    void businessUnitUpdate_preservesExistingQuotaUsage() {
        BusinessUnit existing = BusinessUnit.builder()
                .id("bu-001")
                .organizationId(ORGANIZATION_ID)
                .name("Digital Banking")
                .code("DIGITAL-BANKING")
                .status(BusinessUnitStatus.ACTIVE)
                .quotas(List.of(BusinessUnitQuota.builder()
                        .quotaType("api_calls")
                        .quotaLimit(new BigDecimal("1000"))
                        .quotaUsed(new BigDecimal("275"))
                        .build()))
                .build();
        when(businessUnitRepository.findByIdAndOrganizationIdAndDeletedAtIsNull("bu-001", ORGANIZATION_ID))
                .thenReturn(Optional.of(existing));
        when(businessUnitRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        BusinessUnitService service = new BusinessUnitService(businessUnitRepository, projectRepository,
                applicationRepository, linkRepository, auditService, new PagingService(), accessControlService);
        BusinessUnitUpdateRequest request = new BusinessUnitUpdateRequest();
        request.setQuotas(List.of(BusinessUnitQuota.builder()
                .quotaType("api_calls")
                .quotaLimit(new BigDecimal("2000"))
                .quotaUsed(new BigDecimal("999"))
                .build()));

        var response = service.update(ORGANIZATION_ID, "bu-001", request, ACTOR);

        assertEquals(new BigDecimal("2000"), response.getQuotas().get(0).getQuotaLimit());
        assertEquals(new BigDecimal("275"), response.getQuotas().get(0).getQuotaUsed());
    }

    @Test
    void projectCreate_mapsSectionsAndUsesIsEnabledJsonProperty() throws Exception {
        BusinessUnit businessUnit = BusinessUnit.builder().id("bu-001").organizationId(ORGANIZATION_ID)
                .name("Digital Banking").status(BusinessUnitStatus.ACTIVE).build();
        when(businessUnitService.find(ORGANIZATION_ID, "bu-001")).thenReturn(businessUnit);
        when(projectRepository.save(any())).thenAnswer(invocation -> {
            OnboardingProject value = invocation.getArgument(0);
            value.setId("project-001");
            return value;
        });
        ProjectService service = new ProjectService(projectRepository, applicationRepository, linkRepository,
                businessUnitService, auditService, new PagingService(), accessControlService);
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setBusinessUnitId("bu-001");
        request.setName("Payments Modernization");
        request.setCode("payments");
        request.setProjectManagerId("pm-1");
        request.setMethodology("Scrum");
        request.setJwtEnabled(true);
        request.setEnvironments(List.of(ProjectEnvironment.builder()
                .environmentType("Production").endpointUrl(" https://payments.example.com ").enabled(true).build()));
        request.setOwaspTop10Enabled(true);

        var response = service.create(ORGANIZATION_ID, request, ACTOR);

        assertEquals("pm-1", response.getProjectManagerId());
        assertEquals("Scrum", response.getMethodology());
        assertTrue(response.isJwtEnabled());
        assertEquals("https://payments.example.com", response.getEnvironments().get(0).getEndpointUrl());
        String json = new ObjectMapper().writeValueAsString(response.getEnvironments().get(0));
        assertTrue(json.contains("\"isEnabled\":true"));
        assertFalse(json.contains("\"enabled\""));
    }

    @Test
    void applicationCreate_mapsRuntimeAiAgentSecurityAndBillingFields() {
        BusinessUnit businessUnit = BusinessUnit.builder().id("bu-001").organizationId(ORGANIZATION_ID).name("Digital Banking").build();
        OnboardingProject project = OnboardingProject.builder().id("project-001").organizationId(ORGANIZATION_ID)
                .businessUnitId("bu-001").name("Payments").build();
        when(businessUnitService.find(ORGANIZATION_ID, "bu-001")).thenReturn(businessUnit);
        when(projectService.find(ORGANIZATION_ID, "project-001")).thenReturn(project);
        AtomicReference<OnboardingApplication> saved = new AtomicReference<>();
        when(applicationRepository.save(any())).thenAnswer(invocation -> {
            OnboardingApplication value = invocation.getArgument(0);
            value.setId("app-001");
            saved.set(value);
            return value;
        });
        when(applicationRepository.findByIdAndOrganizationIdAndDeletedAtIsNull("app-001", ORGANIZATION_ID))
                .thenAnswer(invocation -> Optional.of(saved.get()));
        when(linkRepository.findByOrganizationIdAndApplicationId(ORGANIZATION_ID, "app-001")).thenReturn(List.of());
        ApplicationService service = new ApplicationService(applicationRepository, linkRepository, consumerRepository,
                businessUnitService, projectService, auditService, new PagingService(), accessControlService);
        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setBusinessUnitId("bu-001");
        request.setProjectId("project-001");
        request.setName("Payments API");
        request.setApplicationId("payments-api");
        request.setBusinessCapability("Payments");
        request.setRuntime("Kubernetes");
        request.setApiCount(12);
        request.setGraphqlEnabled(true);
        request.setLlmProvider("OpenAI");
        request.setMcpEnabled(true);
        request.setAgentEnabled(true);
        request.setKnowledgeBase("Payments KB");
        request.setOauthEnabled(true);
        request.setDlpEnabled(true);
        request.setMonthlyBudget(new BigDecimal("3500.25"));
        request.setTokenBudget(500000L);

        var response = service.create(ORGANIZATION_ID, request, ACTOR);

        assertEquals("PAYMENTS-API", response.getApplicationId());
        assertEquals("Payments", response.getBusinessCapability());
        assertEquals("Kubernetes", response.getRuntime());
        assertEquals(12, response.getApiCount());
        assertTrue(response.isGraphqlEnabled());
        assertEquals("OpenAI", response.getLlmProvider());
        assertTrue(response.isMcpEnabled());
        assertTrue(response.isAgentEnabled());
        assertEquals("Payments KB", response.getKnowledgeBase());
        assertTrue(response.isOauthEnabled());
        assertTrue(response.isDlpEnabled());
        assertEquals(new BigDecimal("3500.25"), response.getMonthlyBudget());
        assertEquals(500000L, response.getTokenBudget());
    }
}
