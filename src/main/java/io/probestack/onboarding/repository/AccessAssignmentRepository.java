package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.AccessAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessAssignmentRepository extends MongoRepository<AccessAssignment, String> {
    List<AccessAssignment> findByOrganizationIdAndPrincipalEmailIgnoreCaseAndActiveTrue(String organizationId, String principalEmail);
    List<AccessAssignment> findByOrganizationIdAndActiveTrue(String organizationId);
    List<AccessAssignment> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
}
