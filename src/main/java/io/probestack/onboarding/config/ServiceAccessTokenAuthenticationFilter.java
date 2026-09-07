package io.probestack.onboarding.config;

import com.forge.security.authn.model.AuthnToken;
import com.forge.security.authn.security.ForgeAuthnAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ServiceAccessTokenAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final ServiceAccessTokenValidator validator;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public ServiceAccessTokenAuthenticationFilter(
            ServiceAccessTokenValidator validator,
            AuthenticationEntryPoint authenticationEntryPoint) {
        this.validator = validator;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String encodedToken = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!validator.canHandle(encodedToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthnToken authnToken = validator.verify(encodedToken);
            SecurityContextHolder.getContext().setAuthentication(new ForgeAuthnAuthenticationToken(authnToken));
            filterChain.doFilter(request, response);
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Service access token validation failed", ex));
        }
    }
}
