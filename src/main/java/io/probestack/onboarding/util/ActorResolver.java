package io.probestack.onboarding.util;

import com.forge.security.authn.model.AuthnToken;
import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.exception.ForbiddenOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ActorResolver {
    private static final String ORGANIZATION_ID_CLAIM = "organization_id";
    private static final String EMAIL_CLAIM = "email";
    private static final String NAME_CLAIM = "name";
    private static final String ROLE_CLAIM = "role";
    private static final String SERVICE_TOKEN_TYPE = "PROBESTACK_SERVICE_ACCESS";

    public String requireOrganizationId(HttpServletRequest request) {
        AuthnToken token = requireAuthnToken();
        String organizationId = firstText(
                stringClaim(token, ORGANIZATION_ID_CLAIM),
                stringClaim(token, "userOrgId"),
                stringClaim(token, "backendOrgId"));
        if (!StringUtils.hasText(organizationId)) {
            throw new ForbiddenOperationException("Authenticated token must contain an organization_id claim");
        }
        return organizationId;
    }

    public Actor requireActor(ActorDTO requestActor, HttpServletRequest request) {
        Actor actor = resolveActor(requestActor, request);
        if (!StringUtils.hasText(actor.email()) && !StringUtils.hasText(actor.userId())) {
            throw new ForbiddenOperationException("Authenticated token must contain a user identity");
        }
        return actor;
    }

    public Actor resolveActor(ActorDTO requestActor, HttpServletRequest request) {
        AuthnToken token = requireAuthnToken();
        String userId = firstText(token.getSubject(), stringClaim(token, "userId"), stringClaim(token, "admin_id"));
        String email = firstText(stringClaim(token, EMAIL_CLAIM), stringClaim(token, "userEmail"));
        String name = firstText(stringClaim(token, NAME_CLAIM), stringClaim(token, "userName"), email, userId, "User");
        String role = firstText(stringClaim(token, ROLE_CLAIM), stringClaim(token, "userRole"), "USER");
        String tokenType = firstText(stringClaim(token, "token_type"), stringClaim(token, "tokenType"));
        String principalType = stringClaim(token, "principal_type");
        String clientId = firstText(
                stringClaim(token, "client_id"),
                stringClaim(token, "azp"),
                stringClaim(token, "cid"));
        Set<String> scopes = scopes(token);
        return new Actor(
                trimToNull(userId),
                trimToNull(email),
                trimToNull(name),
                normalizeRole(role),
                normalizeCode(principalType),
                trimToNull(clientId),
                normalizeCode(tokenType),
                scopes);
    }

    private AuthnToken requireAuthnToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getDetails() instanceof AuthnToken authnToken)) {
            throw new ForbiddenOperationException("A validated authentication token is required");
        }
        return authnToken;
    }

    private String stringClaim(AuthnToken token, String name) {
        Object value = token.getClaim(name);
        return value instanceof String text ? text : null;
    }

    private Set<String> scopes(AuthnToken token) {
        Set<String> scopes = new LinkedHashSet<>();
        addScopes(scopes, token.getClaim("scope"));
        addScopes(scopes, token.getClaim("scp"));
        return Set.copyOf(scopes);
    }

    private void addScopes(Set<String> target, Object claim) {
        if (claim instanceof String text) {
            for (String scope : text.trim().split("[\\s,]+")) {
                if (StringUtils.hasText(scope)) target.add(scope.trim());
            }
            return;
        }
        if (claim instanceof Collection<?> values) {
            values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(target::add);
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return value.trim();
        }
        return null;
    }

    private String normalizeRole(String role) {
        String normalized = StringUtils.hasText(role) ? role.trim().replaceAll("[-\\s]+", "_").toUpperCase() : "USER";
        if (Set.of("ORG_ADMIN", "ORGANIZATION_ADMIN", "OWNER", "ADMIN").contains(normalized)) return "ORG_ADMIN";
        return Set.of("BUSINESS_UNIT_ADMIN", "PROJECT_ADMIN", "APPLICATION_OWNER", "APPLICATION_MEMBER", "MODERATOR", "USER").contains(normalized) ? normalized : "USER";
    }

    private String normalizeCode(String value) {
        return StringUtils.hasText(value)
                ? value.trim().replaceAll("[-\\s]+", "_").toUpperCase(Locale.ROOT)
                : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record Actor(
            String userId,
            String email,
            String name,
            String role,
            String principalType,
            String clientId,
            String tokenType,
            Set<String> scopes) {
        public Actor(String userId, String email, String name, String role) {
            this(userId, email, name, role, null, null, null, Set.of());
        }

        public Actor {
            scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
        }

        public String key() {
            return StringUtils.hasText(userId) ? userId : email;
        }

        public boolean presentsServiceCredentials() {
            return SERVICE_TOKEN_TYPE.equals(tokenType)
                    || (principalType != null && Set.of("SERVICE", "USER_DELEGATION").contains(principalType));
        }

        public boolean hasScope(String requiredScope) {
            return scopes.contains(requiredScope);
        }
    }
}

