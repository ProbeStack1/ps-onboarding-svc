package io.probestack.onboarding;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.forge.security.authn.model.AuthnToken;
import com.forge.security.authn.security.ForgeAuthnAuthenticationToken;
import io.probestack.onboarding.exception.ForbiddenOperationException;
import io.probestack.onboarding.util.ActorResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActorResolverTest {
    private static final Algorithm TEST_ALGORITHM =
            Algorithm.HMAC256("test-only-secret-that-is-long-enough");

    private final ActorResolver actorResolver = new ActorResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsOrganizationAndActorOnlyFromValidatedTokenClaims() {
        authenticate(JWT.create()
                .withSubject("d23e26c4-fc12-4553-8b28-bb4a6fdad564")
                .withClaim("organization_id", "f52c02e6-d67a-4bc9-8e94-36e9d4b8d38c")
                .withClaim("email", "admin@forgecrux.com")
                .withClaim("name", "admin@forgecrux.com")
                .withClaim("role", "org_admin")
                .sign(TEST_ALGORITHM));
        MockHttpServletRequest request = spoofedHeaderRequest();

        assertEquals("f52c02e6-d67a-4bc9-8e94-36e9d4b8d38c", actorResolver.requireOrganizationId(request));
        assertEquals(
                new ActorResolver.Actor(
                        "d23e26c4-fc12-4553-8b28-bb4a6fdad564",
                        "admin@forgecrux.com",
                        "admin@forgecrux.com",
                        "ORG_ADMIN"),
                actorResolver.requireActor(null, request));
    }

    @Test
    void doesNotFallBackToIdentityHeadersWithoutValidatedToken() {
        MockHttpServletRequest request = spoofedHeaderRequest();

        assertThrows(ForbiddenOperationException.class, () -> actorResolver.requireOrganizationId(request));
        assertThrows(ForbiddenOperationException.class, () -> actorResolver.requireActor(null, request));
    }

    @Test
    void requiresOrganizationClaimEvenWhenHeaderProvidesOne() {
        authenticate(JWT.create()
                .withSubject("user-1")
                .withClaim("email", "user@forgecrux.com")
                .sign(TEST_ALGORITHM));

        assertThrows(ForbiddenOperationException.class,
                () -> actorResolver.requireOrganizationId(spoofedHeaderRequest()));
    }

    private void authenticate(String encodedToken) {
        AuthnToken authnToken = new AuthnToken(JWT.decode(encodedToken));
        SecurityContextHolder.getContext().setAuthentication(new ForgeAuthnAuthenticationToken(authnToken));
    }

    private MockHttpServletRequest spoofedHeaderRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Organization-Id", "spoofed-organization");
        request.addHeader("X-User-Id", "spoofed-user");
        request.addHeader("X-User-Email", "spoofed@example.com");
        request.addHeader("X-User-Name", "Spoofed User");
        request.addHeader("X-User-Role", "USER");
        return request;
    }
}
