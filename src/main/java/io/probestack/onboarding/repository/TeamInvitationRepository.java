package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.InvitationStatus;
import io.probestack.onboarding.model.TeamInvitation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInvitationRepository extends MongoRepository<TeamInvitation, String> {
    Optional<TeamInvitation> findByIdAndOrganizationId(String id, String organizationId);
    List<TeamInvitation> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
    List<TeamInvitation> findByOrganizationIdAndTeamIdOrderByCreatedAtDesc(String organizationId, String teamId);
    List<TeamInvitation> findByOrganizationIdAndInvitedEmailIgnoreCaseOrderByCreatedAtDesc(String organizationId, String invitedEmail);
    List<TeamInvitation> findByOrganizationIdAndStatusAndAcceptedByEmailIgnoreCaseOrderByCreatedAtDesc(String organizationId, InvitationStatus status, String acceptedByEmail);
}
