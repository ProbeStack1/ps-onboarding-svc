package io.probestack.onboarding.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServiceAccessTokenValidationProperties.class)
@ConditionalOnProperty(prefix = "onboarding.service-token-validation", name = "enabled", havingValue = "true")
public class ServiceAccessTokenValidationConfiguration {
    @Bean
    ServiceAccessTokenValidator serviceAccessTokenValidator(
            ServiceAccessTokenValidationProperties properties,
            ObjectMapper objectMapper) {
        return new ServiceAccessTokenValidator(properties, objectMapper);
    }

    @Bean
    ServiceAccessTokenAuthenticationFilter serviceAccessTokenAuthenticationFilter(
            ServiceAccessTokenValidator validator,
            AuthenticationEntryPoint authenticationEntryPoint) {
        return new ServiceAccessTokenAuthenticationFilter(validator, authenticationEntryPoint);
    }

    @Bean
    FilterRegistrationBean<ServiceAccessTokenAuthenticationFilter> disableStandaloneServiceTokenFilterRegistration(
            ServiceAccessTokenAuthenticationFilter filter) {
        FilterRegistrationBean<ServiceAccessTokenAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
