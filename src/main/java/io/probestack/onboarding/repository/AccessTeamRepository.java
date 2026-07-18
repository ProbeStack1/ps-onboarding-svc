package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.AccessTeam;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessTeamRepository extends MongoRepository<AccessTeam, String> {
    Optional<AccessTeam> findByIdAndOrganizationIdAndDeletedAtIsNull(String id, String organizationId);
    List<AccessTeam> findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(String organizationId);
}
