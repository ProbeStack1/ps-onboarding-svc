package io.probestack.onboarding;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.forge.security.authn.model.AuthnToken;
import com.forge.security.authn.validator.AuthnValidator;
import io.probestack.onboarding.config.AuthenticationSecurityConfig;
import io.probestack.onboarding.controller.OrganizationMemberController;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.dto.member.OrganizationMemberResponse;
import io.probestack.onboarding.service.OrganizationMemberService;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OrganizationMemberController.class, properties = "forge.authn.enabled=true")
@Import({AuthenticationSecurityConfig.class, ActorResolver.class,
        OrganizationMemberControllerSecurityTest.SecurityErrorHandlerConfig.class})
class OrganizationMemberControllerSecurityTest {
    private static final String ORG = "org-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthnValidator authnValidator;

    @MockBean
    private OrganizationMemberService memberService;

    @MockBean(name = "mongoMappingContext")
    private MongoMappingContext mongoMappingContext;

    @Test
    void cookieAuthenticationClaimsAndAdaptedBearerReachMemberDirectory() throws Exception {
        AuthnToken authnToken = token();
        ActorResolver.Actor actor = new ActorResolver.Actor("admin-1", "admin@example.com", "Admin", "ORG_ADMIN");
        when(authnValidator.validate("Bearer cookie-token")).thenReturn(authnToken);
        when(memberService.list(ORG, null, "ACTIVE", 0, 20, "Bearer cookie-token", actor))
                .thenReturn(PagedResult.<OrganizationMemberResponse>builder()
                        .items(List.of(OrganizationMemberResponse.builder()
                                .principalId("member-1").organizationId(ORG).email("member@example.com")
                                .name("Member").active(true).roles(List.of()).build()))
                        .totalElements(1)
                        .build());

        mockMvc.perform(get("/api/v1/onboarding/organization-members")
                        .cookie(new Cookie("ps_auth_token", "cookie-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].principalId").value("member-1"))
                .andExpect(jsonPath("$.meta.totalElements").value(1));

        verify(authnValidator).validate("Bearer cookie-token");
        verify(memberService).list(
                eq(ORG), eq(null), eq("ACTIVE"), eq(0), eq(20), eq("Bearer cookie-token"), eq(actor));
    }

    private AuthnToken token() {
        String encoded = JWT.create()
                .withIssuer("https://auth.probestack.io")
                .withAudience("probestack-api")
                .withSubject("admin-1")
                .withClaim("organization_id", ORG)
                .withClaim("email", "admin@example.com")
                .withClaim("name", "Admin")
                .withClaim("role", "org_admin")
                .sign(Algorithm.HMAC256("test-only-secret-that-is-long-enough"));
        return new AuthnToken(JWT.decode(encoded));
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
