package io.probestack.onboarding.dto.application;

import io.probestack.onboarding.dto.common.ActorDTO;
import io.probestack.onboarding.model.ApplicationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ApplicationUpdateRequest {
    private String businessUnitId;
    private String projectId;
    private String name;
    private String applicationId;
    private String displayName;
    private String description;
    private String businessCapability;
    private String domain;
    private String applicationType;
    @Pattern(regexp = "^(Critical|High|Medium|Low)$", message = "Criticality is not supported")
    private String criticality;
    private String runtime;
    private String language;
    private String framework;
    private String version;
    private String containerImage;
    private String kubernetesNamespace;
    private String cluster;
    @Min(value = 0, message = "API count must be zero or greater")
    private Integer apiCount;
    private String apiGateway;
    private String baseUrl;
    private String openapiSpecUrl;
    private String asyncapiSpecUrl;
    private Boolean graphqlEnabled;
    private Boolean webhooksEnabled;
    private String llmProvider;
    private String defaultModel;
    private String embeddingModel;
    private String aiGateway;
    private String vectorDatabase;
    private String promptRegistry;
    private Boolean mcpEnabled;
    private String mcpServer;
    private String mcpResources;
    private String mcpTools;
    private String mcpPrompts;
    private Boolean agentEnabled;
    private String planner;
    private String executor;
    private String memory;
    private String knowledgeBase;
    private Boolean multiAgentEnabled;
    private String workflow;
    private String logging;
    private String metrics;
    private String tracing;
    private String alerts;
    private String dashboards;
    private Boolean oauthEnabled;
    private Boolean jwtEnabled;
    private Boolean apiKeyEnabled;
    private Boolean mtlsEnabled;
    private Boolean dlpEnabled;
    private Boolean wafEnabled;
    private String encryptionStandard;
    private String costCenter;
    @DecimalMin(value = "0.0", message = "Monthly budget must be zero or greater")
    private BigDecimal monthlyBudget;
    @Min(value = 0, message = "Token budget must be zero or greater")
    private Long tokenBudget;
    @Min(value = 0, message = "API budget must be zero or greater")
    private Long apiBudget;
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
