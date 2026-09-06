package io.probestack.onboarding;

import io.probestack.onboarding.exception.ForbiddenOperationException;
import io.probestack.onboarding.service.ServiceTokenAuthorizer;
import io.probestack.onboarding.util.ActorResolver;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceTokenAuthorizerTest {
    private final ServiceTokenAuthorizer authorizer =
            new ServiceTokenAuthorizer(true, "probestack-admin-backend,another-service");

    @Test
    void authorizesTrustedServiceWithRequiredScope() {
        assertThat(authorizer.authorizeIfService(
                serviceActor("probestack-admin-backend", Set.of(ServiceTokenAuthorizer.ACCESS_READ)),
                ServiceTokenAuthorizer.ACCESS_READ)).isTrue();
    }

    @Test
    void leavesExistingUserTokensOnTheExistingRbacPath() {
        ActorResolver.Actor user = new ActorResolver.Actor(
                "user-1", "admin@example.com", "Admin", "ORG_ADMIN");

        assertThat(authorizer.authorizeIfService(user, ServiceTokenAuthorizer.ACCESS_READ)).isFalse();
    }

    @Test
    void rejectsUntrustedServiceClient() {
        assertThatThrownBy(() -> authorizer.authorizeIfService(
                serviceActor("untrusted-service", Set.of(ServiceTokenAuthorizer.ACCESS_READ)),
                ServiceTokenAuthorizer.ACCESS_READ))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("not trusted");
    }

    @Test
    void rejectsServiceWithoutRequiredScope() {
        assertThatThrownBy(() -> authorizer.authorizeIfService(
                serviceActor("probestack-admin-backend", Set.of(ServiceTokenAuthorizer.MEMBERS_READ)),
                ServiceTokenAuthorizer.ACCESS_READ))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining(ServiceTokenAuthorizer.ACCESS_READ);
    }

    private ActorResolver.Actor serviceActor(String clientId, Set<String> scopes) {
        return new ActorResolver.Actor(
                "service:" + clientId,
                null,
                clientId,
                "USER",
                "SERVICE",
                clientId,
                "PROBESTACK_SERVICE_ACCESS",
                scopes);
    }
}
