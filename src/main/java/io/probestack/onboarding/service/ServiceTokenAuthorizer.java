package io.probestack.onboarding.service;

import io.probestack.onboarding.exception.ForbiddenOperationException;
import io.probestack.onboarding.util.ActorResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authorizes already-authenticated service principals for the admin-only API surface.
 * JWT signature, issuer, audience and lifetime validation are performed by forge-auth-lib or
 * the dedicated service-token issuer's RSA/JWKS filter. Human context tokens deliberately fall through
 * to the existing RBAC path.
 */
@Component
public class ServiceTokenAuthorizer {
    public static final String MEMBERS_READ = "onboarding:members:read";
    public static final String ACCESS_READ = "onboarding:access:read";
    public static final String BOOTSTRAP_READ = "onboarding:bootstrap:read";
    public static final String ASSIGNMENTS_READ = "onboarding:assignments:read";
    public static final String ASSIGNMENTS_WRITE = "onboarding:assignments:write";
    public static final String BUSINESS_UNITS_READ = "onboarding:business-units:read";
    public static final String BUSINESS_UNITS_WRITE = "onboarding:business-units:write";
    public static final String PROJECTS_READ = "onboarding:projects:read";
    public static final String PROJECTS_WRITE = "onboarding:projects:write";
    public static final String APPLICATIONS_READ = "onboarding:applications:read";
    public static final String APPLICATIONS_WRITE = "onboarding:applications:write";
    public static final String CONSUMERS_READ = "onboarding:consumers:read";
    public static final String CONSUMERS_WRITE = "onboarding:consumers:write";
    public static final String DEVELOPERS_READ = "onboarding:developers:read";
    public static final String DEVELOPERS_WRITE = "onboarding:developers:write";
    public static final String TEAMS_READ = "onboarding:teams:read";
    public static final String TEAMS_WRITE = "onboarding:teams:write";

    private static final String SERVICE_TOKEN_TYPE = "PROBESTACK_SERVICE_ACCESS";
    private static final Set<String> SERVICE_PRINCIPAL_TYPES = Set.of("SERVICE", "USER_DELEGATION");

    private final boolean enabled;
    private final Set<String> trustedClients;

    public ServiceTokenAuthorizer(
            @Value("${onboarding.service-auth.enabled:true}") boolean enabled,
            @Value("${onboarding.service-auth.trusted-clients:probestack-admin-backend}") String trustedClients) {
        this.enabled = enabled;
        this.trustedClients = Arrays.stream((trustedClients == null ? "" : trustedClients).split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @return true when a service principal is authorized; false for a normal user token.
     * @throws ForbiddenOperationException when service credentials are presented but invalid.
     */
    public boolean authorizeIfService(ActorResolver.Actor actor, String requiredScope) {
        if (actor == null || !actor.presentsServiceCredentials()) return false;
        if (!enabled) throw forbidden("Service-token access is disabled");
        if (!SERVICE_TOKEN_TYPE.equals(actor.tokenType())) {
            throw forbidden("Service token has an invalid token_type");
        }
        if (actor.principalType() == null || !SERVICE_PRINCIPAL_TYPES.contains(actor.principalType())) {
            throw forbidden("Service token has an invalid principal_type");
        }
        if (!StringUtils.hasText(actor.clientId()) || !trustedClients.contains(actor.clientId())) {
            throw forbidden("Service client is not trusted for onboarding admin APIs");
        }
        if (!actor.hasScope(requiredScope)) {
            throw forbidden("Service token is missing required scope: " + requiredScope);
        }
        return true;
    }

    private ForbiddenOperationException forbidden(String message) {
        return new ForbiddenOperationException(message);
    }
}
