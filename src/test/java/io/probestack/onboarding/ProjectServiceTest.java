package io.probestack.onboarding;

import io.probestack.onboarding.dto.project.ProjectCreateRequest;
import io.probestack.onboarding.exception.InvalidStatusTransitionException;
import io.probestack.onboarding.model.BusinessUnit;
import io.probestack.onboarding.model.BusinessUnitStatus;
import io.probestack.onboarding.repository.ApplicationConsumerLinkRepository;
import io.probestack.onboarding.repository.ApplicationRepository;
import io.probestack.onboarding.repository.ProjectRepository;
import io.probestack.onboarding.service.AccessControlService;
import io.probestack.onboarding.service.AuditService;
import io.probestack.onboarding.service.BusinessUnitService;
import io.probestack.onboarding.service.PagingService;
import io.probestack.onboarding.service.ProjectService;
import io.probestack.onboarding.util.ActorResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationConsumerLinkRepository linkRepository;
    @Mock
    private BusinessUnitService businessUnitService;
    @Mock
    private AuditService auditService;
    @Mock
    private AccessControlService accessControlService;

    @Test
    void createProject_requiresActiveBusinessUnit() {
        ProjectService service = new ProjectService(projectRepository, applicationRepository, linkRepository,
                businessUnitService, auditService, new PagingService(), accessControlService);
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setBusinessUnitId("bu-001");
        request.setName("Inventory Modernization");
        request.setCode("INV-MOD");
        request.setOwnerName("Jagruti");

        when(businessUnitService.find("org-001", "bu-001")).thenReturn(BusinessUnit.builder()
                .id("bu-001")
                .organizationId("org-001")
                .name("Wealth Management")
                .code("WEALTH")
                .status(BusinessUnitStatus.DRAFT)
                .build());

        assertThrows(InvalidStatusTransitionException.class, () -> service.create("org-001", request,
                new ActorResolver.Actor("user-1", "admin@example.com", "Admin", "ADMIN")));
    }
}