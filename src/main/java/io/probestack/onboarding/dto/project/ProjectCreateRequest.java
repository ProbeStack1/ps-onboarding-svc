package io.probestack.onboarding.dto.project;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.ProjectStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectCreateRequest {
    @NotBlank(message = "Business unit id is required")
    private String businessUnitId;
    @NotBlank(message = "Project name is required")
    private String name;
    @NotBlank(message = "Project code is required")
    private String code;
    @NotBlank(message = "Project owner is required")
    private String ownerName;
    @Email(message = "Owner email must be valid")
    private String ownerEmail;
    @Email(message = "Project DL email must be valid")
    private String projectDlEmail;
    private LocalDate expectedGoLiveDate;
    private String deliveryModel;
    private ProjectStatus status = ProjectStatus.READY;
    private ActorDTO actor;
}
