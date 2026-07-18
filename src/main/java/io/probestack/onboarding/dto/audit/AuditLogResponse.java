package io.probestack.onboarding.dto.audit;

import io.probestack.onboarding.model.AuditAction;
import io.probestack.onboarding.model.FieldChange;
import io.probestack.onboarding.model.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private String id;
    private String organizationId;
    private ResourceType resourceType;
    private String resourceId;
    private AuditAction action;
    private String actorUserId;
    private String actorEmail;
    private String actorName;
    private String actorRole;
    private List<FieldChange> changedFields;
    private Map<String, Object> snapshotBefore;
    private Map<String, Object> snapshotAfter;
    private Instant createdAt;
}
