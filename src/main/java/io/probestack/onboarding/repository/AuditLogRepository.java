package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.AuditLog;
import io.probestack.onboarding.model.ResourceType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findByOrganizationIdAndResourceTypeAndResourceIdOrderByCreatedAtDesc(String organizationId, ResourceType resourceType, String resourceId);
    List<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
}
