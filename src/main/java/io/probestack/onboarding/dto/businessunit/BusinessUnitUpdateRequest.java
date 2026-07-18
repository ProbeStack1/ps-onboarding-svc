package io.probestack.onboarding.dto.businessunit;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.BusinessUnitStatus;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class BusinessUnitUpdateRequest {
    private String name;
    private String code;
    private String ownerName;
    @Email(message = "Owner email must be valid")
    private String ownerEmail;
    private String costCenter;
    private String description;
    private BusinessUnitStatus status;
    private ActorDTO actor;
}
