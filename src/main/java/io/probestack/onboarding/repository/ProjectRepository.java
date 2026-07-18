package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.OnboardingProject;
import io.probestack.onboarding.model.ProjectStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends MongoRepository<OnboardingProject, String> {
    List<OnboardingProject> findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId);
    List<OnboardingProject> findByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId, String businessUnitId);
    List<OnboardingProject> findByOrganizationIdAndBusinessUnitIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId, String businessUnitId, ProjectStatus status);
    Optional<OnboardingProject> findByIdAndOrganizationIdAndDeletedAtIsNull(String id, String organizationId);
    boolean existsByOrganizationIdAndBusinessUnitIdAndCode(String organizationId, String businessUnitId, String code);
    long countByOrganizationIdAndDeletedAtIsNull(String organizationId);
    long countByOrganizationIdAndBusinessUnitIdAndDeletedAtIsNull(String organizationId, String businessUnitId);
}
