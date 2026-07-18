package io.probestack.onboarding.dto.application;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.ApplicationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApplicationCreateRequest {
    @NotBlank(message = "Business unit id is required")
    private String businessUnitId;
    @NotBlank(message = "Project id is required")
    private String projectId;
    @NotBlank(message = "Application name is required")
    private String name;
    @NotBlank(message = "Application id is required")
    private String applicationId;
    @NotBlank(message = "Application owner is required")
    private String ownerName;
    @Email(message = "Owner email must be valid")
    private String ownerEmail;
    private String applicationSme;
    @Email(message = "SME email must be valid")
    private String smeEmail;
    private String testerName;
    @Email(message = "Tester email must be valid")
    private String testerEmail;
    private String serviceNowGroupName;
    @Email(message = "ServiceNow email must be valid")
    private String serviceNowEmail;
    private ApplicationStatus status = ApplicationStatus.DRAFT;
    private List<String> consumerIds = new ArrayList<>();
    private ActorDTO actor;
}
