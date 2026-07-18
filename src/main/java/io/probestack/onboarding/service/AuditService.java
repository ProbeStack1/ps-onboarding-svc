package io.probestack.onboarding.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.model.AuditAction;
import io.probestack.onboarding.model.AuditLog;
import io.probestack.onboarding.model.FieldChange;
import io.probestack.onboarding.model.ResourceType;
import io.probestack.onboarding.repository.AuditLogRepository;
import io.probestack.onboarding.util.ActorResolver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void record(String organizationId, ResourceType resourceType, String resourceId, AuditAction action,
                       ActorResolver.Actor actor, List<FieldChange> changedFields, Object before, Object after) {
        auditLogRepository.save(AuditLog.builder()
                .organizationId(organizationId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .action(action)
                .actorUserId(actor == null ? null : actor.userId())
                .actorEmail(actor == null ? null : actor.email())
                .actorName(actor == null ? null : actor.name())
                .actorRole(actor == null ? null : actor.role())
                .changedFields(changedFields == null ? List.of() : changedFields)
                .snapshotBefore(toMap(before))
                .snapshotAfter(toMap(after))
                .build());
    }

    public List<AuditLogResponse> history(String organizationId, ResourceType resourceType, String resourceId) {
        return auditLogRepository.findByOrganizationIdAndResourceTypeAndResourceIdOrderByCreatedAtDesc(organizationId, resourceType, resourceId)
                .stream().map(this::toResponse).toList();
    }

    public List<AuditLogResponse> all(String organizationId) {
        return auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream().map(this::toResponse).toList();
    }

    public Map<String, Object> toMap(Object value) {
        if (value == null) return null;
        return objectMapper.convertValue(value, new TypeReference<>() {});
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .organizationId(log.getOrganizationId())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .action(log.getAction())
                .actorUserId(log.getActorUserId())
                .actorEmail(log.getActorEmail())
                .actorName(log.getActorName())
                .actorRole(log.getActorRole())
                .changedFields(log.getChangedFields())
                .snapshotBefore(log.getSnapshotBefore())
                .snapshotAfter(log.getSnapshotAfter())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
