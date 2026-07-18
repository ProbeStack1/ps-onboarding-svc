package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.BusinessUnit;
import io.probestack.onboarding.model.BusinessUnitStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessUnitRepository extends MongoRepository<BusinessUnit, String> {
    List<BusinessUnit> findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId);
    List<BusinessUnit> findByOrganizationIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId, BusinessUnitStatus status);
    Optional<BusinessUnit> findByIdAndOrganizationIdAndDeletedAtIsNull(String id, String organizationId);
    boolean existsByOrganizationIdAndCode(String organizationId, String code);
    long countByOrganizationIdAndDeletedAtIsNull(String organizationId);
}
