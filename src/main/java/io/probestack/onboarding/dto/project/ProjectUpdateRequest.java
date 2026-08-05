package io.probestack.onboarding.dto.project;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.ProjectEnvironment;
import io.probestack.onboarding.model.ProjectStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectUpdateRequest {
    private String businessUnitId;
    private String name;
    private String code;
    private String description;
    private String projectType;
    private String portfolio;
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
    private Boolean mtlsEnabled;
    private Boolean jwtEnabled;
    private Boolean apiKeyEnabled;
    private String secretsVault;
    @Valid
    private List<ProjectEnvironment> environments;
    private Boolean pciApplicable;
    private String standardRules;
    private String customRules;
    private Boolean owaspTop10Enabled;
    private Boolean lintingEnabled;
    private ProjectStatus status;
    private ActorDTO actor;
}
