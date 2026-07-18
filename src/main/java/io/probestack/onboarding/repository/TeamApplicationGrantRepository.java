package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.TeamApplicationGrant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamApplicationGrantRepository extends MongoRepository<TeamApplicationGrant, String> {
    List<TeamApplicationGrant> findByOrganizationIdAndTeamId(String organizationId, String teamId);
    List<TeamApplicationGrant> findByOrganizationIdAndTeamIdIn(String organizationId, List<String> teamIds);
    List<TeamApplicationGrant> findByOrganizationIdAndApplicationId(String organizationId, String applicationId);
    boolean existsByOrganizationIdAndTeamIdAndApplicationId(String organizationId, String teamId, String applicationId);
    void deleteByOrganizationIdAndTeamIdAndApplicationId(String organizationId, String teamId, String applicationId);
    void deleteByOrganizationIdAndTeamId(String organizationId, String teamId);
}
