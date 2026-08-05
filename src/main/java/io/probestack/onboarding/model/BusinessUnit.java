package io.probestack.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "onboarding_business_units")
@CompoundIndexes({
        @CompoundIndex(name = "bu_org_code_uidx", def = "{'organizationId': 1, 'code': 1}", unique = true),
        @CompoundIndex(name = "bu_org_status_idx", def = "{'organizationId': 1, 'status': 1, 'updatedAt': -1}")
})
public class BusinessUnit {
    @Id
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
    @Builder.Default
    private List<String> regulatoryStandards = new ArrayList<>();
    private String retentionPolicy;
    private String backupPolicy;
    private boolean drEnabled;
    private String slaTier;
    @Builder.Default
    private List<BusinessUnitQuota> quotas = new ArrayList<>();
    private String description;
    @Builder.Default
    private BusinessUnitStatus status = BusinessUnitStatus.ACTIVE;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private String deletedBy;
    private String deletedByEmail;
    private Instant deletedAt;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
