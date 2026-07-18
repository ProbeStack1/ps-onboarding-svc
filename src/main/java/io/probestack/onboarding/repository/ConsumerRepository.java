package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.Consumer;
import io.probestack.onboarding.model.ConsumerStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumerRepository extends MongoRepository<Consumer, String> {
    List<Consumer> findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId);
    List<Consumer> findByOrganizationIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(String organizationId, ConsumerStatus status);
    List<Consumer> findByOrganizationIdAndIdInAndDeletedAtIsNull(String organizationId, List<String> ids);
    Optional<Consumer> findByIdAndOrganizationIdAndDeletedAtIsNull(String id, String organizationId);
    long countByOrganizationIdAndDeletedAtIsNull(String organizationId);
}
