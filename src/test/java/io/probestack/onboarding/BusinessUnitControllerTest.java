package io.probestack.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.probestack.onboarding.controller.BusinessUnitController;
import io.probestack.onboarding.controller.GlobalExceptionHandler;
import io.probestack.onboarding.dto.businessunit.BusinessUnitCreateRequest;
import io.probestack.onboarding.dto.businessunit.BusinessUnitResponse;
import io.probestack.onboarding.model.BusinessUnitStatus;
import io.probestack.onboarding.service.BusinessUnitService;
import io.probestack.onboarding.util.ActorResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessUnitController.class)
@Import(GlobalExceptionHandler.class)
class BusinessUnitControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BusinessUnitService businessUnitService;

    @MockBean
    private ActorResolver actorResolver;

    @MockBean(name = "mongoMappingContext")
    private MongoMappingContext mongoMappingContext;

    @Test
    void createBusinessUnit_validPayload_returns201Envelope() throws Exception {
        BusinessUnitCreateRequest request = new BusinessUnitCreateRequest();
        request.setName("Wealth Management");
        request.setCode("WEALTH");
        request.setOwnerName("Jagruti");
        request.setOwnerEmail("owner@example.com");

        when(actorResolver.requireOrganizationId(any())).thenReturn("org-001");
        when(actorResolver.requireActor(any(), any())).thenReturn(new ActorResolver.Actor("user-1", "admin@example.com", "Admin", "ADMIN"));
        when(businessUnitService.create(eq("org-001"), any(), any())).thenReturn(BusinessUnitResponse.builder()
                .id("bu-001")
                .organizationId("org-001")
                .name("Wealth Management")
                .code("WEALTH")
                .status(BusinessUnitStatus.ACTIVE)
                .build());

        mockMvc.perform(post("/api/v1/onboarding/business-units")
                        .header("X-Organization-Id", "org-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value("ONBOARDING_201"))
                .andExpect(jsonPath("$.data.id").value("bu-001"));
    }

    @Test
    void createBusinessUnit_missingName_returnsValidationEnvelope() throws Exception {
        BusinessUnitCreateRequest request = new BusinessUnitCreateRequest();
        request.setCode("WEALTH");
        request.setOwnerName("Jagruti");

        mockMvc.perform(post("/api/v1/onboarding/business-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
