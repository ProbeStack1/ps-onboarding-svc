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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AdminBackendOrganizationMemberClient implements OrganizationMemberClient {
    private static final Logger log = LoggerFactory.getLogger(AdminBackendOrganizationMemberClient.class);
    private static final List<String> ARRAY_FIELDS = List.of("accounts", "users", "members", "items", "content", "results");

    private final RestClient restClient;
    private final String accountsPath;

    public AdminBackendOrganizationMemberClient(
            RestClient.Builder builder,
            @Value("${onboarding.members.admin-api.base-url:https://probestack.io/admin-backend}") String baseUrl,
            @Value("${onboarding.members.admin-api.accounts-path:/api/accounts}") String accountsPath) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("onboarding.members.admin-api.base-url must not be blank");
        }
        this.restClient = builder.baseUrl(baseUrl.trim()).build();
        this.accountsPath = normalizePath(accountsPath);
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
                        var builder = uriBuilder.path(accountsPath)
                                .queryParam("organization", organizationId)
                                .queryParam("page", Math.max(page, 0))
                                .queryParam("size", Math.max(1, Math.min(size, 200)));
                        if (StringUtils.hasText(search)) builder.queryParam("search", search.trim());
                        if (StringUtils.hasText(status)) builder.queryParam("status", status.trim());
                        return builder.build();
                    });
            if (StringUtils.hasText(authorization)) {
                request = request.header(HttpHeaders.AUTHORIZATION, authorization.trim());
            }
            JsonNode body = request.retrieve().body(JsonNode.class);
            return parse(body);
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
        String principalId = firstText(item, "principalId", "principal_id", "id", "_id", "userId", "user_id", "sub");
        if (!StringUtils.hasText(principalId) && admin != null) {
            principalId = firstText(admin, "principalId", "id", "_id", "userId", "user_id");
        }
        if (!StringUtils.hasText(principalId) && user != null) {
            principalId = firstText(user, "principalId", "id", "_id", "userId", "user_id", "sub");
        }
        String email = firstText(item, "email", "userEmail", "user_email");
        if (!StringUtils.hasText(email) && admin != null) email = firstText(admin, "email", "userEmail", "user_email");
        if (!StringUtils.hasText(email) && user != null) email = firstText(user, "email", "userEmail", "user_email");
        if (!StringUtils.hasText(principalId) || !StringUtils.hasText(email)) {
            log.debug("event=adminMemberSkipped|reason=missingPrincipalOrEmail");
            return null;
        }
        String name = firstText(item, "name", "fullName", "full_name", "userName", "username");
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
                normalizeCode(role, "USER"),
                normalizeCode(accountStatus, active ? "ACTIVE" : "INACTIVE"),
                active);
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
        return StringUtils.hasText(value)
                ? value.trim().replaceAll("[-\\s]+", "_").toUpperCase(Locale.ROOT)
                : fallback;
    }

    private String normalizePath(String value) {
        if (!StringUtils.hasText(value)) return "/api/accounts";
        String path = value.trim();
        return path.startsWith("/") ? path : "/" + path;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
