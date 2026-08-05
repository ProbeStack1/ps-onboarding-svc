package io.probestack.onboarding.dto.businessunit;

import io.probestack.onboarding.model.BusinessUnitStatus;
import io.probestack.onboarding.model.BusinessUnitQuota;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessUnitResponse {
    private String id;
    private String organizationId;
    private String name;
    private String code;
    private String displayName;
    private String parentBusinessUnitId;
    private String division;
    private String department;
    private String lineOfBusiness;
    private String ownerName;
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
    private BigDecimal budget;
    private String chargebackModel;
    private String billingAccount;
    private BigDecimal monthlyBudget;
    private BigDecimal annualBudget;
    private BigDecimal aiBudget;
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
    private String riskClassification;
    private String businessCriticality;
    private String dataClassification;
    private List<String> regulatoryStandards;
    private String retentionPolicy;
    private String backupPolicy;
    private boolean drEnabled;
    private String slaTier;
    private List<BusinessUnitQuota> quotas;
    private String description;
    private BusinessUnitStatus status;
    private long projectCount;
    private long applicationCount;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
