package io.probestack.onboarding.dto.project;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.ProjectStatus;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectUpdateRequest {
    private String businessUnitId;
    private String name;
    private String code;
    private String ownerName;
    @Email(message = "Owner email must be valid")
    private String ownerEmail;
    @Email(message = "Project DL email must be valid")
    private String projectDlEmail;
    private LocalDate expectedGoLiveDate;
    private String deliveryModel;
    private ProjectStatus status;
    private ActorDTO actor;
}
