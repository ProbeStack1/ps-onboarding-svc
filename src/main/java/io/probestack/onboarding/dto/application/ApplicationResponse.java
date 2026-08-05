package io.probestack.onboarding.dto.application;

import io.probestack.onboarding.model.ApplicationStatus;
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
public class ApplicationResponse {
    private String id;
    private String organizationId;
    private String businessUnitId;
    private String businessUnitName;
    private String projectId;
    private String projectName;
    private String name;
    private String applicationId;
    private String displayName;
    private String description;
    private String businessCapability;
    private String domain;
    private String applicationType;
    private String criticality;
    private String runtime;
    private String language;
    private String framework;
    private String version;
    private String containerImage;
    private String kubernetesNamespace;
    private String cluster;
    private Integer apiCount;
    private String apiGateway;
    private String baseUrl;
    private String openapiSpecUrl;
    private String asyncapiSpecUrl;
    private boolean graphqlEnabled;
    private boolean webhooksEnabled;
    private String llmProvider;
    private String defaultModel;
    private String embeddingModel;
    private String aiGateway;
    private String vectorDatabase;
    private String promptRegistry;
    private boolean mcpEnabled;
    private String mcpServer;
    private String mcpResources;
    private String mcpTools;
    private String mcpPrompts;
    private boolean agentEnabled;
    private String planner;
    private String executor;
    private String memory;
    private String knowledgeBase;
    private boolean multiAgentEnabled;
    private String workflow;
    private String logging;
    private String metrics;
    private String tracing;
    private String alerts;
    private String dashboards;
    private boolean oauthEnabled;
    private boolean jwtEnabled;
    private boolean apiKeyEnabled;
    private boolean mtlsEnabled;
    private boolean dlpEnabled;
    private boolean wafEnabled;
    private String encryptionStandard;
    private String costCenter;
    private BigDecimal monthlyBudget;
    private Long tokenBudget;
    private Long apiBudget;
    private String ownerName;
    private String ownerEmail;
    private String applicationSme;
    private String smeEmail;
    private String testerName;
    private String testerEmail;
    private String serviceNowGroupName;
    private String serviceNowEmail;
    private ApplicationStatus status;
    private long consumerCount;
    private List<String> consumerIds;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
