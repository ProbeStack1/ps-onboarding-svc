package io.probestack.onboarding.client;

public record OrganizationMemberRecord(
        String principalId,
        String email,
        String name,
        String organizationRole,
        String accountStatus,
        boolean active) {
}
