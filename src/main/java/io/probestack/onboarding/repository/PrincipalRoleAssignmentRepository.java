package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.PrincipalRoleAssignment;
import io.probestack.onboarding.model.RoleKind;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PrincipalRoleAssignmentRepository extends MongoRepository<PrincipalRoleAssignment, String> {
    List<PrincipalRoleAssignment> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
    List<PrincipalRoleAssignment> findByOrganizationIdAndActiveTrueOrderByCreatedAtDesc(String organizationId);
    Optional<PrincipalRoleAssignment> findByIdAndOrganizationId(String id, String organizationId);
    boolean existsByOrganizationIdAndPrincipalIdAndRoleKindAndRoleCodeAndScopeTypeAndScopeIdAndActiveTrue(
            String organizationId,
            String principalId,
            RoleKind roleKind,
            String roleCode,
            String scopeType,
            String scopeId);
}
