package io.probestack.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.probestack.onboarding.config.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonConfigTest {

    @Test
    void applicationObjectMapperSerializesInstant() throws Exception {
        ObjectMapper objectMapper = new JacksonConfig()
                .applicationObjectMapper(Jackson2ObjectMapperBuilder.json());

        assertEquals(
                "{\"timestamp\":\"2026-09-04T05:23:07Z\"}",
                objectMapper.writeValueAsString(
                        Map.of("timestamp", Instant.parse("2026-09-04T05:23:07Z"))));
    }
}
