package io.probestack.onboarding.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AdminBackendOrganizationMemberClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AdminBackendOrganizationMemberClient client = new AdminBackendOrganizationMemberClient(
            RestClient.builder(), "https://admin.example.test",
            "/api/organizations/{organizationId}/users-with-roles");

    @Test
    void parsesNestedAdminAccountPage() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "success": true,
                  "data": {
                    "accounts": [
                      {
                        "admin": {
                          "id": "user-101",
                          "email": "Owner@Example.com",
                          "name": "Application Owner",
                          "role": "org-admin",
                          "is_active": true
                        }
                      }
                    ],
                    "pagination": { "totalElements": 37 }
                  }
                }
                """);

        OrganizationMemberClient.MemberPage page = client.parse(payload);

        assertThat(page.totalElements()).isEqualTo(37);
        assertThat(page.items()).containsExactly(new OrganizationMemberRecord(
                "user-101", "owner@example.com", "Application Owner", "ORG_ADMIN", "ACTIVE", true));
    }

    @Test
    void parsesUsersWithRolesAndUsesEmailWhenTheProviderDoesNotReturnAnId() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                [
                  {
                    "user_name": "Admin Owner",
                    "user_email": "Admin@Example.com",
                    "org_role": "Org Admin / Owner",
                    "business_units": [],
                    "mongodb_role_lookup": null
                  },
                  {
                    "user_name": "BU Admin",
                    "user_email": "bu.admin@example.com",
                    "org_role": "Business Unit Admin",
                    "business_units": []
                  }
                ]
                """);

        OrganizationMemberClient.MemberPage page = client.parse(payload);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.items().get(0)).isEqualTo(new OrganizationMemberRecord(
                "admin@example.com", "admin@example.com", "Admin Owner", "ORG_ADMIN", "ACTIVE", true));
        assertThat(page.items().get(1).organizationRole()).isEqualTo("BUSINESS_UNIT_ADMIN");
    }

    @Test
    void excludesMalformedAccountsInsteadOfCreatingUnresolvableMembers() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {"items":[{"id":"missing-email"},{"id":"valid","email":"valid@example.com","status":"suspended"}]}
                """);

        OrganizationMemberClient.MemberPage page = client.parse(payload);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).principalId()).isEqualTo("valid");
        assertThat(page.items().get(0).active()).isFalse();
    }

    @Test
    void callsOrganizationUsersWithRolesAndForwardsBearerAndCookie() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AdminBackendOrganizationMemberClient httpClient = new AdminBackendOrganizationMemberClient(
                builder, "https://admin.example.test",
                "/api/organizations/{organizationId}/users-with-roles");
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(
                        "https://admin.example.test/api/organizations/org-1/users-with-roles?")))
                .andExpect(queryParam("status", "active"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer cookie-token"))
                .andExpect(header(HttpHeaders.COOKIE, "ps_auth_token=cookie-token"))
                .andRespond(withSuccess("""
                        [
                          {"user_name":"Application Owner","user_email":"owner@example.com","org_role":"Designer"},
                          {"user_name":"Another User","user_email":"another@example.com","org_role":"Designer"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        OrganizationMemberClient.MemberPage result = httpClient.fetchMembers(
                "org-1", 0, 25, "owner", "ACTIVE", "Bearer cookie-token");

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.items()).extracting(OrganizationMemberRecord::email)
                .containsExactly("owner@example.com");
        server.verify();
    }
}
