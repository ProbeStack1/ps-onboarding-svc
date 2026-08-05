package io.probestack.onboarding.dto.project;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.ProjectEnvironment;
import io.probestack.onboarding.model.ProjectStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

@Data
public class ProjectCreateRequest {
    @NotBlank(message = "Business unit id is required")
    private String businessUnitId;
    @NotBlank(message = "Project name is required")
    private String name;
    @NotBlank(message = "Project code is required")
    private String code;
    private String description;
    private String projectType;
    private String portfolio;
    @NotBlank(message = "Project owner is required")
    private String ownerName;
    @Email(message = "Owner email must be valid")
    private String ownerEmail;
    @Email(message = "Project DL email must be valid")
    private String projectDlEmail;
    private String projectManagerId;
    private String productManagerId;
    private String scrumMasterId;
    private String technicalLeadId;
    private String securityLeadId;
    private String devopsLeadId;
    private LocalDate expectedGoLiveDate;
    private String deliveryModel;
    @Pattern(regexp = "^(Scrum|Kanban|Waterfall|SAFe|Hybrid)$", message = "Methodology is not supported")
    private String methodology;
    private String sprintDuration;
    private String repository;
    private String cicdTool;
    private String issueTracker;
    private String documentationUrl;
    private String authenticationMethod;
    private String authorizationMethod;
    private String oauthProvider;
    private boolean mtlsEnabled;
    private boolean jwtEnabled;
    private boolean apiKeyEnabled;
    private String secretsVault;
    @Valid
    private List<ProjectEnvironment> environments = new ArrayList<>();
    private boolean pciApplicable;
    private String standardRules;
    private String customRules;
    private boolean owaspTop10Enabled;
    private boolean lintingEnabled;
    private ProjectStatus status = ProjectStatus.READY;
    private ActorDTO actor;
}
