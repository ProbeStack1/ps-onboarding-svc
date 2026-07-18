package io.probestack.onboarding.repository;

import io.probestack.onboarding.model.ApplicationConsumerLink;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationConsumerLinkRepository extends MongoRepository<ApplicationConsumerLink, String> {
    List<ApplicationConsumerLink> findByOrganizationIdAndApplicationId(String organizationId, String applicationId);
    List<ApplicationConsumerLink> findByOrganizationIdAndApplicationIdIn(String organizationId, List<String> applicationIds);
    void deleteByOrganizationIdAndApplicationIdAndConsumerIdIn(String organizationId, String applicationId, List<String> consumerIds);
    long countByOrganizationIdAndApplicationId(String organizationId, String applicationId);
}
