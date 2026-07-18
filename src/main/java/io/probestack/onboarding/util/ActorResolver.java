package io.probestack.onboarding.util;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.exception.ForbiddenOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class ActorResolver {
    public static final String ORGANIZATION_HEADER = "X-Organization-Id";

    public String requireOrganizationId(HttpServletRequest request) {
        String organizationId = trimToNull(header(request, ORGANIZATION_HEADER));
        if (!StringUtils.hasText(organizationId)) {
            throw new ForbiddenOperationException("X-Organization-Id header is required");
        }
        return organizationId;
    }

    public Actor requireActor(ActorDTO requestActor, HttpServletRequest request) {
        Actor actor = resolveActor(requestActor, request);
        if (!StringUtils.hasText(actor.email()) && !StringUtils.hasText(actor.userId())) {
            throw new ForbiddenOperationException("User identity is required for this onboarding action");
        }
        return actor;
    }

    public Actor resolveActor(ActorDTO requestActor, HttpServletRequest request) {
        String userId = firstText(header(request, "X-User-Id"), requestActor == null ? null : requestActor.getUserId());
        String email = firstText(header(request, "X-User-Email"), requestActor == null ? null : requestActor.getEmail());
        String name = firstText(header(request, "X-User-Name"), requestActor == null ? null : requestActor.getName(), email, userId, "User");
        String role = firstText(header(request, "X-User-Role"), requestActor == null ? null : requestActor.getRole(), "USER");
        return new Actor(trimToNull(userId), trimToNull(email), trimToNull(name), normalizeRole(role));
    }

    private String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
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

