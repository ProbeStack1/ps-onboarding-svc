package io.probestack.onboarding.dto.businessunit;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.BusinessUnitQuota;
import io.probestack.onboarding.model.BusinessUnitStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BusinessUnitUpdateRequest {
    private String name;
    private String code;
    private String displayName;
    private String parentBusinessUnitId;
    private String division;
    private String department;
    private String lineOfBusiness;
    private String ownerName;
    @Email(message = "Owner email must be valid")
    private String ownerEmail;
    private String businessExecutiveId;
    private String businessOwnerId;
    private String productOwnerId;
    private String technicalOwnerId;
    private String enterpriseArchitectId;
    private String platformOwnerId;
    private String securityOwnerId;
    private String complianceOfficerId;
    private String supportTeam;
    private String operationsTeam;
    private String costCenter;
    @DecimalMin(value = "0.0", message = "Budget must be zero or greater")
    private BigDecimal budget;
    private String chargebackModel;
    private String billingAccount;
    @DecimalMin(value = "0.0", message = "Monthly budget must be zero or greater")
    private BigDecimal monthlyBudget;
    @DecimalMin(value = "0.0", message = "Annual budget must be zero or greater")
    private BigDecimal annualBudget;
    @DecimalMin(value = "0.0", message = "AI budget must be zero or greater")
    private BigDecimal aiBudget;
    @DecimalMin(value = "0.0", message = "API budget must be zero or greater")
    private BigDecimal apiBudget;
    private String cloudProvider;
    private String region;
    private String kubernetesCluster;
    private String namespace;
    private String apiGateway;
    private String aiGateway;
    private String loggingPlatform;
    private String monitoringPlatform;
    private String secretManager;
    private String approvalWorkflow;
    @Pattern(regexp = "^(Critical|High|Medium|Low)$", message = "Risk classification is not supported")
    private String riskClassification;
    @Pattern(regexp = "^(Critical|High|Medium|Low)$", message = "Business criticality is not supported")
    private String businessCriticality;
    @Pattern(regexp = "^(Public|Internal|Confidential|Restricted)$", message = "Data classification is not supported")
    private String dataClassification;
    private List<String> regulatoryStandards;
    private String retentionPolicy;
    private String backupPolicy;
    private Boolean drEnabled;
    @Pattern(regexp = "^(Platinum|Gold|Silver|Bronze)$", message = "SLA tier is not supported")
    private String slaTier;
    @Valid
    private List<BusinessUnitQuota> quotas;
    private String description;
    private BusinessUnitStatus status;
    private ActorDTO actor;
}
