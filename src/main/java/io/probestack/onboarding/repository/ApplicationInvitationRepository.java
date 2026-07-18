package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.ApplicationInvitation;
import io.probestack.onboarding.model.InvitationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationInvitationRepository extends MongoRepository<ApplicationInvitation, String> {
    Optional<ApplicationInvitation> findByIdAndOrganizationId(String id, String organizationId);
    List<ApplicationInvitation> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);
    List<ApplicationInvitation> findByOrganizationIdAndStatusOrderByCreatedAtDesc(String organizationId, InvitationStatus status);
    List<ApplicationInvitation> findByOrganizationIdAndInvitedEmailIgnoreCaseOrderByCreatedAtDesc(String organizationId, String invitedEmail);
    List<ApplicationInvitation> findByOrganizationIdAndApplicationIdOrderByCreatedAtDesc(String organizationId, String applicationId);
    List<ApplicationInvitation> findByOrganizationIdAndStatusAndAcceptedByEmailIgnoreCaseOrderByCreatedAtDesc(String organizationId, InvitationStatus status, String acceptedByEmail);
}
