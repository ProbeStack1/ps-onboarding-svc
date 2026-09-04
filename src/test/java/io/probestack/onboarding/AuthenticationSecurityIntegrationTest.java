package io.probestack.onboarding;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.security.authn.model.AuthnToken;
import com.forge.security.authn.validator.AuthnValidator;
import io.probestack.onboarding.config.AuthenticationSecurityConfig;
import io.probestack.onboarding.controller.BusinessUnitController;
import io.probestack.onboarding.dto.businessunit.BusinessUnitCreateRequest;
import io.probestack.onboarding.dto.businessunit.BusinessUnitResponse;
import io.probestack.onboarding.model.BusinessUnitStatus;
import io.probestack.onboarding.service.BusinessUnitService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BusinessUnitController.class, properties = "forge.authn.enabled=true")
@Import({AuthenticationSecurityConfig.class, ActorResolver.class,
        AuthenticationSecurityIntegrationTest.SecurityErrorHandlerConfig.class})
class AuthenticationSecurityIntegrationTest {
    private static final String ORGANIZATION_ID = "f52c02e6-d67a-4bc9-8e94-36e9d4b8d38c";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthnValidator authnValidator;

    @MockBean
    private BusinessUnitService businessUnitService;

    @MockBean(name = "mongoMappingContext")
    private MongoMappingContext mongoMappingContext;

    @Test
    void protectedEndpointRejectsRequestWithoutBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/business-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authnValidator, businessUnitService);
    }

    @Test
    void protectedEndpointUsesValidatedClaimsAndIgnoresSpoofedIdentityHeaders() throws Exception {
        AuthnToken token = tokenWithPastedClaims();
        when(authnValidator.validate("Bearer valid-context-token")).thenReturn(token);
        when(businessUnitService.create(eq(ORGANIZATION_ID), any(), any())).thenReturn(
                BusinessUnitResponse.builder()
                        .id("bu-001")
                        .organizationId(ORGANIZATION_ID)
                        .name("Wealth Management")
                        .code("WEALTH")
                        .status(BusinessUnitStatus.ACTIVE)
                        .build());

        mockMvc.perform(post("/api/v1/onboarding/business-units")
                        .header("Authorization", "Bearer valid-context-token")
                        .header("X-Organization-Id", "spoofed-organization")
                        .header("X-User-Id", "spoofed-user")
                        .header("X-User-Email", "spoofed@example.com")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.organizationId").value(ORGANIZATION_ID));

        verify(authnValidator).validate("Bearer valid-context-token");
        verify(businessUnitService).create(
                eq(ORGANIZATION_ID),
                any(),
                eq(new ActorResolver.Actor(
                        "d23e26c4-fc12-4553-8b28-bb4a6fdad564",
                        "admin@forgecrux.com",
                        "admin@forgecrux.com",
                        "ORG_ADMIN")));
    }

    @Test
    void protectedEndpointAcceptsPsAuthTokenCookie() throws Exception {
        AuthnToken token = tokenWithPastedClaims();
        when(authnValidator.validate("Bearer cookie-context-token")).thenReturn(token);
        when(businessUnitService.create(eq(ORGANIZATION_ID), any(), any())).thenReturn(
                BusinessUnitResponse.builder()
                        .id("bu-002")
                        .organizationId(ORGANIZATION_ID)
                        .name("Wealth Management")
                        .code("WEALTH")
                        .status(BusinessUnitStatus.ACTIVE)
                        .build());

        mockMvc.perform(post("/api/v1/onboarding/business-units")
                        .cookie(new Cookie("ps_auth_token", "cookie-context-token"))
                        .header("Authorization", "Bearer ignored-header-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.organizationId").value(ORGANIZATION_ID));

        verify(authnValidator).validate("Bearer cookie-context-token");
        verify(businessUnitService).create(
                eq(ORGANIZATION_ID),
                any(),
                eq(new ActorResolver.Actor(
                        "d23e26c4-fc12-4553-8b28-bb4a6fdad564",
                        "admin@forgecrux.com",
                        "admin@forgecrux.com",
                        "ORG_ADMIN")));
    }

    private AuthnToken tokenWithPastedClaims() {
        String encodedToken = JWT.create()
                .withIssuer("https://auth.probestack.io")
                .withAudience("probestack-api", "probestack-ui")
                .withSubject("d23e26c4-fc12-4553-8b28-bb4a6fdad564")
                .withClaim("organization_id", ORGANIZATION_ID)
                .withClaim("email", "admin@forgecrux.com")
                .withClaim("name", "admin@forgecrux.com")
                .withClaim("role", "org_admin")
                .sign(Algorithm.HMAC256("test-only-secret-that-is-long-enough"));
        return new AuthnToken(JWT.decode(encodedToken));
    }

    private BusinessUnitCreateRequest validRequest() {
        BusinessUnitCreateRequest request = new BusinessUnitCreateRequest();
        request.setName("Wealth Management");
        request.setCode("WEALTH");
        request.setOwnerName("Jagruti");
        request.setOwnerEmail("owner@example.com");
        return request;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityErrorHandlerConfig {
        @Bean
        @Primary
        AuthenticationEntryPoint testAuthenticationEntryPoint() {
            return (request, response, exception) -> response.sendError(401);
        }

        @Bean
        @Primary
        AccessDeniedHandler testAccessDeniedHandler() {
            return (request, response, exception) -> response.sendError(403);
        }
    }
}
