package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.DeveloperAccountStatus;
import io.probestack.onboarding.model.OnboardingDeveloper;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperRepository extends MongoRepository<OnboardingDeveloper, String> {
    List<OnboardingDeveloper> findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId);
    List<OnboardingDeveloper> findByOrganizationIdAndAccountStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId, DeveloperAccountStatus status);
    Optional<OnboardingDeveloper> findByIdAndOrganizationIdAndDeletedAtIsNull(String id, String organizationId);
    boolean existsByOrganizationIdAndEmail(String organizationId, String email);
    boolean existsByOrganizationIdAndUsername(String organizationId, String username);
    boolean existsByOrganizationIdAndEmployeeId(String organizationId, String employeeId);
}
