package io.probestack.onboarding.util;

import com.forge.security.authn.model.AuthnToken;
import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.exception.ForbiddenOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class ActorResolver {
    private static final String ORGANIZATION_ID_CLAIM = "organization_id";
    private static final String EMAIL_CLAIM = "email";
    private static final String NAME_CLAIM = "name";
    private static final String ROLE_CLAIM = "role";

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
        return new Actor(trimToNull(userId), trimToNull(email), trimToNull(name), normalizeRole(role));
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record Actor(String userId, String email, String name, String role) {
        public String key() {
            return StringUtils.hasText(userId) ? userId : email;
        }
    }
}

