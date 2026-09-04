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
            RestClient.builder(), "https://admin.example.test", "/api/accounts");

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
    void sendsOrganizationFiltersAndTheAdaptedBearerToAdminBackend() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AdminBackendOrganizationMemberClient httpClient = new AdminBackendOrganizationMemberClient(
                builder, "https://admin.example.test", "/api/accounts");
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://admin.example.test/api/accounts?")))
                .andExpect(queryParam("organization", "org-1"))
                .andExpect(queryParam("page", "2"))
                .andExpect(queryParam("size", "25"))
                .andExpect(queryParam("search", "owner"))
                .andExpect(queryParam("status", "ACTIVE"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer cookie-token"))
                .andRespond(withSuccess("{\"items\":[],\"totalElements\":0}", MediaType.APPLICATION_JSON));

        OrganizationMemberClient.MemberPage result = httpClient.fetchMembers(
                "org-1", 2, 25, "owner", "ACTIVE", "Bearer cookie-token");

        assertThat(result.items()).isEmpty();
        server.verify();
    }
}
