package io.probestack.onboarding.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapts the browser authentication cookie to the bearer header contract expected by
 * forge-auth-lib. A valid cookie takes precedence over a supplied Authorization header.
 */
final class CookieBearerTokenFilter extends OncePerRequestFilter {
    static final String AUTH_COOKIE_NAME = "ps_auth_token";
    private static final Logger log = LoggerFactory.getLogger(CookieBearerTokenFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String cookieToken = cookieToken(request);
        if (cookieToken == null) {
            log.debug("authSource=cookie|event=notFound|cookieName={}|authorizationHeaderPresent={}|method={}|path={}",
                    AUTH_COOKIE_NAME,
                    request.getHeader(HttpHeaders.AUTHORIZATION) != null,
                    request.getMethod(),
                    request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String bearerValue = cookieToken.startsWith("Bearer ")
                ? cookieToken
                : "Bearer " + cookieToken;
        log.debug("authSource=cookie|event=adaptedToBearer|cookieName={}|method={}|path={}",
                AUTH_COOKIE_NAME, request.getMethod(), request.getRequestURI());
        filterChain.doFilter(new AuthorizationHeaderRequest(request, bearerValue), response);
        if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            log.warn("authSource=cookie|event=rejected|cookieName={}|method={}|path={}",
                    AUTH_COOKIE_NAME, request.getMethod(), request.getRequestURI());
        }
    }

    private String cookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (!AUTH_COOKIE_NAME.equals(cookie.getName())) continue;
            String value = cookie.getValue();
            if (value == null || value.isBlank() || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                return null;
            }
            return value.trim();
        }
        return null;
    }

    private static final class AuthorizationHeaderRequest extends HttpServletRequestWrapper {
        private final String authorization;

        private AuthorizationHeaderRequest(HttpServletRequest request, String authorization) {
            super(request);
            this.authorization = authorization;
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) return authorization;
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return Collections.enumeration(Collections.singletonList(authorization));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> existing = super.getHeaderNames();
            if (existing != null) existing.asIterator().forEachRemaining(names::add);
            names.add(HttpHeaders.AUTHORIZATION);
            return Collections.enumeration(names);
        }
    }
}
