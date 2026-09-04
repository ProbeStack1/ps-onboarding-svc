package io.probestack.onboarding.config;

import com.forge.security.authn.security.ForgeAuthnAuthenticationFilter;
import com.forge.security.authn.validator.AuthnValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;

@Configuration(proxyBeanMethods = false)
public class AuthenticationSecurityConfig {
    private static final String[] PUBLIC_PATHS = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    @ConditionalOnProperty(prefix = "forge.authn", name = "enabled", havingValue = "true", matchIfMissing = true)
    ForgeAuthnAuthenticationFilter forgeAuthnAuthenticationFilter(
            AuthnValidator authnValidator,
            AuthenticationEntryPoint authenticationEntryPoint) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        return new ForgeAuthnAuthenticationFilter(authnValidator, authenticationEntryPoint) {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;

                String path = pathWithinApplication(request);
                for (String publicPath : PUBLIC_PATHS) {
                    if (pathMatcher.match(publicPath, path)) return true;
                }
                return false;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "forge.authn", name = "enabled", havingValue = "true", matchIfMissing = true)
    FilterRegistrationBean<ForgeAuthnAuthenticationFilter> disableStandaloneAuthFilterRegistration(
            ForgeAuthnAuthenticationFilter authenticationFilter) {
        FilterRegistrationBean<ForgeAuthnAuthenticationFilter> registration =
                new FilterRegistrationBean<>(authenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "forge.authn", name = "enabled", havingValue = "true", matchIfMissing = true)
    CookieBearerTokenFilter cookieBearerTokenFilter() {
        return new CookieBearerTokenFilter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "forge.authn", name = "enabled", havingValue = "true", matchIfMissing = true)
    FilterRegistrationBean<CookieBearerTokenFilter> disableStandaloneCookieFilterRegistration(
            CookieBearerTokenFilter cookieBearerTokenFilter) {
        FilterRegistrationBean<CookieBearerTokenFilter> registration =
                new FilterRegistrationBean<>(cookieBearerTokenFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "forge.authn", name = "enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain authenticationSecurityFilterChain(
            HttpSecurity http,
            CookieBearerTokenFilter cookieBearerTokenFilter,
            ForgeAuthnAuthenticationFilter authenticationFilter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(cookieBearerTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(authenticationFilter, CookieBearerTokenFilter.class);
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "forge.authn", name = "enabled", havingValue = "false")
    SecurityFilterChain disabledAuthenticationSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }
}
