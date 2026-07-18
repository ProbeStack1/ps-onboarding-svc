package io.probestack.onboarding.dto.businessunit;

import io.probestack.onboarding.model.BusinessUnitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessUnitResponse {
    private String id;
    private String organizationId;
    private String name;
    private String code;
    private String ownerName;
    private String ownerEmail;
    private String costCenter;
    private String description;
    private BusinessUnitStatus status;
    private long projectCount;
    private long applicationCount;
    private String createdBy;
    private String createdByEmail;
    private String updatedBy;
    private String updatedByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
