package io.probestack.onboarding.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.probestack.onboarding.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class AdminBackendOrganizationMemberClient implements OrganizationMemberClient {
    private static final Logger log = LoggerFactory.getLogger(AdminBackendOrganizationMemberClient.class);
    private static final String AUTH_COOKIE_NAME = "ps_auth_token";
    private static final List<String> ARRAY_FIELDS = List.of("accounts", "users", "members", "items", "content", "results");

    private final RestClient restClient;
    private final String usersPath;

    public AdminBackendOrganizationMemberClient(
            RestClient.Builder builder,
            @Value("${onboarding.members.admin-api.base-url:https://probestack.io/admin-backend}") String baseUrl,
            @Value("${onboarding.members.admin-api.users-path:/api/organizations/%s/users-with-roles}") String usersPath) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("onboarding.members.admin-api.base-url must not be blank");
        }
        this.restClient = builder.baseUrl(baseUrl.trim()).build();
        this.usersPath = normalizePath(usersPath);
    }

    @Override
    public MemberPage fetchMembers(
            String organizationId,
            int page,
            int size,
            String search,
            String status,
            String authorization) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(resolveUsersPath(organizationId));
                        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                            builder.queryParam("status", status.trim().toLowerCase(Locale.ROOT));
                        }
                        return builder.build();
                    });
            if (StringUtils.hasText(authorization)) {
                String header = authorization.trim();
                request = request
                        .header(HttpHeaders.AUTHORIZATION, header)
                        .header(HttpHeaders.COOKIE, AUTH_COOKIE_NAME + "=" + bearerValue(header));
            }
            JsonNode body = request.retrieve().body(JsonNode.class);
            return paginate(filter(parse(body).items(), search, status), page, size);
        } catch (RestClientResponseException ex) {
            log.warn("event=adminMemberFetchFailed|organizationId={}|upstreamStatus={}|reason={}",
                    organizationId, ex.getStatusCode().value(), ex.getStatusText());
            throw new UpstreamServiceException(
                    "Unable to load organization members from the admin backend (HTTP "
                            + ex.getStatusCode().value() + ")",
                    ex);
        } catch (RestClientException ex) {
            log.warn("event=adminMemberFetchFailed|organizationId={}|reason={}", organizationId, ex.getMessage());
            throw new UpstreamServiceException("Unable to load organization members from the admin backend", ex);
        }
    }

    MemberPage parse(JsonNode root) {
        JsonNode itemsNode = findItems(root);
        if (itemsNode == null) {
            throw new UpstreamServiceException("Admin backend member response does not contain an account list");
        }
        List<OrganizationMemberRecord> items = new ArrayList<>();
        for (JsonNode item : itemsNode) {
            OrganizationMemberRecord member = toMember(item);
            if (member != null) items.add(member);
        }
        long total = firstLong(root, "totalElements", "total", "totalCount", "count");
        if (total < 0) total = items.size();
        return new MemberPage(List.copyOf(items), total);
    }

    private JsonNode findItems(JsonNode root) {
        if (root == null || root.isNull()) return null;
        if (root.isArray()) return root;
        for (String field : ARRAY_FIELDS) {
            JsonNode candidate = root.get(field);
            if (candidate != null && candidate.isArray()) return candidate;
        }
        for (String container : List.of("data", "result", "payload")) {
            JsonNode nested = root.get(container);
            if (nested == null || nested.isNull()) continue;
            JsonNode found = findItems(nested);
            if (found != null) return found;
        }
        return null;
    }

    private OrganizationMemberRecord toMember(JsonNode item) {
        JsonNode admin = object(item, "admin");
        JsonNode user = object(item, "user");
        String email = firstText(item, "email", "userEmail", "user_email");
        if (!StringUtils.hasText(email) && admin != null) email = firstText(admin, "email", "userEmail", "user_email");
        if (!StringUtils.hasText(email) && user != null) email = firstText(user, "email", "userEmail", "user_email");
        if (!StringUtils.hasText(email)) {
            log.debug("event=adminMemberSkipped|reason=missingEmail");
            return null;
        }
        String principalId = firstText(item, "principalId", "principal_id", "id", "_id", "userId", "user_id", "sub");
        if (!StringUtils.hasText(principalId) && admin != null) {
            principalId = firstText(admin, "principalId", "id", "_id", "userId", "user_id");
        }
        if (!StringUtils.hasText(principalId) && user != null) {
            principalId = firstText(user, "principalId", "id", "_id", "userId", "user_id", "sub");
        }
        if (!StringUtils.hasText(principalId)) principalId = email.trim().toLowerCase(Locale.ROOT);
        String name = firstText(item, "name", "fullName", "full_name", "userName", "user_name", "username");
        if (!StringUtils.hasText(name) && admin != null) name = firstText(admin, "name", "fullName", "userName", "username");
        if (!StringUtils.hasText(name) && user != null) name = firstText(user, "name", "fullName", "userName", "username");
        if (!StringUtils.hasText(name)) {
            String firstName = firstText(item, "firstName", "first_name");
            String lastName = firstText(item, "lastName", "last_name");
            name = String.join(" ", nullToEmpty(firstName), nullToEmpty(lastName)).trim();
        }
        if (!StringUtils.hasText(name)) name = email;
        String role = firstText(item, "organizationRole", "organization_role", "orgRole", "org_role", "role");
        if (!StringUtils.hasText(role) && admin != null) role = firstText(admin, "organizationRole", "orgRole", "role");
        if (!StringUtils.hasText(role) && user != null) role = firstText(user, "organizationRole", "orgRole", "role");
        String accountStatus = firstText(item, "accountStatus", "account_status", "status");
        if (!StringUtils.hasText(accountStatus) && admin != null) accountStatus = firstText(admin, "accountStatus", "status");
        if (!StringUtils.hasText(accountStatus) && user != null) accountStatus = firstText(user, "accountStatus", "status");
        Boolean activeFlag = firstBoolean(item, "active", "isActive", "is_active", "enabled");
        if (activeFlag == null && admin != null) activeFlag = firstBoolean(admin, "active", "isActive", "is_active", "enabled");
        if (activeFlag == null && user != null) activeFlag = firstBoolean(user, "active", "isActive", "is_active", "enabled");
        boolean active = activeFlag != null ? activeFlag : activeStatus(accountStatus);
        return new OrganizationMemberRecord(
                principalId.trim(),
                email.trim().toLowerCase(Locale.ROOT),
                name.trim(),
                normalizeOrganizationRole(role),
                normalizeCode(accountStatus, active ? "ACTIVE" : "INACTIVE"),
                active);
    }

    private List<OrganizationMemberRecord> filter(
            List<OrganizationMemberRecord> members,
            String search,
            String status) {
        String term = StringUtils.hasText(search) ? search.trim().toLowerCase(Locale.ROOT) : null;
        return members.stream()
                .filter(member -> !StringUtils.hasText(status)
                        || "ALL".equalsIgnoreCase(status)
                        || ("ACTIVE".equalsIgnoreCase(status) && member.active())
                        || status.equalsIgnoreCase(member.accountStatus()))
                .filter(member -> term == null
                        || member.principalId().toLowerCase(Locale.ROOT).contains(term)
                        || member.email().toLowerCase(Locale.ROOT).contains(term)
                        || member.name().toLowerCase(Locale.ROOT).contains(term))
                .sorted(Comparator.comparing(OrganizationMemberRecord::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(OrganizationMemberRecord::email, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private MemberPage paginate(List<OrganizationMemberRecord> members, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        long offset = (long) safePage * safeSize;
        int from = offset >= members.size() ? members.size() : (int) offset;
        int to = Math.min(from + safeSize, members.size());
        return new MemberPage(List.copyOf(members.subList(from, to)), members.size());
    }

    private String bearerValue(String authorization) {
        return authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7).trim()
                : authorization;
    }

    private long firstLong(JsonNode root, String... fields) {
        if (root == null || root.isNull()) return -1;
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && value.canConvertToLong()) return value.asLong();
        }
        for (String container : List.of("meta", "page", "pagination", "data", "result")) {
            JsonNode nested = root.get(container);
            long value = firstLong(nested, fields);
            if (value >= 0) return value;
        }
        return -1;
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null) return null;
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isValueNode() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private Boolean firstBoolean(JsonNode node, String... fields) {
        if (node == null) return null;
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) continue;
            if (value.isBoolean()) return value.asBoolean();
            if (value.isTextual()) return Boolean.parseBoolean(value.asText());
        }
        return null;
    }

    private JsonNode object(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isObject() ? value : null;
    }

    private boolean activeStatus(String status) {
        if (!StringUtils.hasText(status)) return true;
        return !List.of("INACTIVE", "DISABLED", "SUSPENDED", "DELETED", "ARCHIVED")
                .contains(normalizeCode(status, ""));
    }

    private String normalizeCode(String value, String fallback) {
        if (!StringUtils.hasText(value)) return fallback;
        return value.trim()
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeOrganizationRole(String value) {
        String role = normalizeCode(value, "USER");
        return switch (role) {
            case "ORG_ADMIN_OWNER", "ORGANIZATION_ADMIN", "ORGANIZATION_ADMIN_OWNER" -> "ORG_ADMIN";
            default -> role;
        };
    }

    private String normalizePath(String value) {
        if (!StringUtils.hasText(value)) return "/api/organizations/%s/users-with-roles";
        String path = value.trim();
        return path.startsWith("/") ? path : "/" + path;
    }

    private String resolveUsersPath(String organizationId) {
        if (usersPath.contains("{organizationId}")) {
            return usersPath.replace("{organizationId}", organizationId);
        }
        if (usersPath.contains("%s")) return usersPath.formatted(organizationId);
        throw new IllegalStateException(
                "onboarding.members.admin-api.users-path must contain %s or {organizationId}");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
