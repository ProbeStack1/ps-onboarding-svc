package io.probestack.onboarding.dto.application;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.ApplicationStatus;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationUpdateRequest {
    private String businessUnitId;
    private String projectId;
    private String name;
    private String applicationId;
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
    private ApplicationStatus status;
    private List<String> consumerIds;
    private ActorDTO actor;
}
