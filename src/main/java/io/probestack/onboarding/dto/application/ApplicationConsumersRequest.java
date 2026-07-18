package io.probestack.onboarding.dto.application;

import io.probestack.onboarding.dto.common.ActorDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApplicationConsumersRequest {
    @NotNull(message = "Consumer ids are required")
    private List<String> consumerIds = new ArrayList<>();
    private ActorDTO actor;
}
