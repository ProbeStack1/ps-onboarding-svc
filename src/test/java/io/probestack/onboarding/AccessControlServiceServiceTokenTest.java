package io.probestack.onboarding;

import io.probestack.onboarding.exception.ForbiddenOperationException;
import io.probestack.onboarding.model.BusinessUnit;
import io.probestack.onboarding.repository.ApplicationConsumerLinkRepository;
import io.probestack.onboarding.service.AccessControlService;
import io.probestack.onboarding.service.MemberAccessResolver;
import io.probestack.onboarding.service.ServiceTokenAuthorizer;
import io.probestack.onboarding.util.ActorResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AccessControlServiceServiceTokenTest {
    private final ApplicationConsumerLinkRepository links = mock(ApplicationConsumerLinkRepository.class);
    private final MemberAccessResolver memberAccessResolver = mock(MemberAccessResolver.class);
    private final ServiceTokenAuthorizer authorizer =
            new ServiceTokenAuthorizer(true, "probestack-admin-backend");
    private final AccessControlService accessControl =
            new AccessControlService(links, memberAccessResolver, authorizer);

    @Test
    void scopedServiceCanReadBusinessUnitsWithoutHumanEmailRbac() {
        List<BusinessUnit> units = List.of(BusinessUnit.builder().id("bu-1").build());

        assertThat(accessControl.filterBusinessUnits(
                "org-1", units, serviceActor(Set.of(ServiceTokenAuthorizer.BUSINESS_UNITS_READ))))
                .isSameAs(units);
        verifyNoInteractions(memberAccessResolver);
    }

    @Test
    void wrongResourceScopeIsRejectedInsteadOfFallingBackToHumanRbac() {
        ActorResolver.Actor actor = serviceActor(Set.of(ServiceTokenAuthorizer.PROJECTS_READ));

        assertThatThrownBy(() -> accessControl.requireBusinessUnitView("org-1", "bu-1", actor))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining(ServiceTokenAuthorizer.BUSINESS_UNITS_READ);
        verifyNoInteractions(memberAccessResolver);
    }

    private ActorResolver.Actor serviceActor(Set<String> scopes) {
        return new ActorResolver.Actor(
                "service:probestack-admin-backend",
                null,
                "ProbeStack Admin Backend",
                "USER",
                "SERVICE",
                "probestack-admin-backend",
                "PROBESTACK_SERVICE_ACCESS",
                scopes);
    }
}
