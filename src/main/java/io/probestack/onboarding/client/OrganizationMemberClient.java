package io.probestack.onboarding.client;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberClient {
    MemberPage fetchMembers(String organizationId, int page, int size, String search, String status, String authorization);

    default Optional<OrganizationMemberRecord> findMember(
            String organizationId,
            String principalId,
            String authorization) {
        return fetchMembers(organizationId, 0, 200, principalId, null, authorization).items().stream()
                .filter(member -> principalId.equals(member.principalId()))
                .findFirst();
    }

    record MemberPage(List<OrganizationMemberRecord> items, long totalElements) {
    }
}
