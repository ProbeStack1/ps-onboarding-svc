package io.probestack.onboarding.dto.project;

import io.probestack.onboarding.model.ProjectStatus;
import io.probestack.onboarding.model.ProjectEnvironment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private String id;
    private String organizationId;
    private String businessUnitId;
    private String businessUnitName;
    private String name;
    private String code;
    private String description;
    private String projectType;
    private String portfolio;
    private String ownerName;
    private String ownerEmail;
    private String projectDlEmail;
    private String projectManagerId;
    private String productManagerId;
    private String scrumMasterId;
    private String technicalLeadId;
    private String securityLeadId;
    private String devopsLeadId;
    private LocalDate expectedGoLiveDate;
    private String deliveryModel;
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
    private List<ProjectEnvironment> environments;
    private boolean pciApplicable;
    private String standardRules;
    private String customRules;
    private boolean owaspTop10Enabled;
    private boolean lintingEnabled;
    private ProjectStatus status;
    private long applicationCount;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
