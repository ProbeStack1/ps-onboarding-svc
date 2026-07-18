package io.probestack.onboarding.dto.businessunit;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.BusinessUnitStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessUnitCreateRequest {
    @NotBlank(message = "Business unit name is required")
    private String name;
    @NotBlank(message = "Business unit code is required")
    private String code;
    @NotBlank(message = "Business owner is required")
    private String ownerName;
    @Email(message = "Owner email must be valid")
    private String ownerEmail;
    private String costCenter;
    private String description;
    private BusinessUnitStatus status = BusinessUnitStatus.ACTIVE;
    private ActorDTO actor;
}
