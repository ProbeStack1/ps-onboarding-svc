package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.ApplicationStatus;
import io.probestack.onboarding.model.OnboardingApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends MongoRepository<OnboardingApplication, String> {
    List<OnboardingApplication> findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId);
    List<OnboardingApplication> findByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId, String businessUnitId);
    List<OnboardingApplication> findByOrganizationIdAndProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId, String projectId);
    List<OnboardingApplication> findByOrganizationIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId, ApplicationStatus status);
    Optional<OnboardingApplication> findByIdAndOrganizationIdAndDeletedAtIsNull(String id, String organizationId);
    boolean existsByOrganizationIdAndApplicationId(String organizationId, String applicationId);
    long countByOrganizationIdAndDeletedAtIsNull(String organizationId);
    long countByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNull(String organizationId, String businessUnitId);
    long countByOrganizationIdAndProjectIdAndDeletedAtIsNull(String organizationId, String projectId);
}
