package io.probestack.onboarding.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Keeps Spring Boot's module-aware JSON configuration authoritative when libraries on the
 * classpath also provide an ObjectMapper fallback.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper applicationObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder
                .createXmlMapper(false)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
