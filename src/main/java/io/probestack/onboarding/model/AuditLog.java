package io.probestack.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "onboarding_audit_logs")
@CompoundIndexes({
        @CompoundIndex(name = "audit_org_resource_idx", def = "{'organizationId': 1, 'resourceType': 1, 'resourceId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "audit_org_action_idx", def = "{'organizationId': 1, 'action': 1, 'createdAt': -1}")
})
public class AuditLog {
    @Id
    private String id;
    private String organizationId;
    private ResourceType resourceType;
    private String resourceId;
    private AuditAction action;
    private String actorUserId;
    private String actorEmail;
    private String actorName;
    private String actorRole;
    @Builder.Default
    private List<FieldChange> changedFields = new ArrayList<>();
    private Map<String, Object> snapshotBefore;
    private Map<String, Object> snapshotAfter;
    @CreatedDate
    private Instant createdAt;
}
