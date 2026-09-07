package io.probestack.onboarding.config;

import com.forge.security.authn.model.AuthnToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceAccessTokenAuthenticationFilterTest {
    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void authenticatesTokensOwnedByTheDedicatedIssuer() throws Exception {
        ServiceAccessTokenValidator validator = mock(ServiceAccessTokenValidator.class);
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        AuthnToken authnToken = mock(AuthnToken.class);
        when(validator.canHandle("issued-token")).thenReturn(true);
        when(validator.verify("issued-token")).thenReturn(authnToken);
        ServiceAccessTokenAuthenticationFilter filter =
                new ServiceAccessTokenAuthenticationFilter(validator, entryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/onboarding/projects");
        request.addHeader("Authorization", "Bearer issued-token");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(entryPoint, never()).commence(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
