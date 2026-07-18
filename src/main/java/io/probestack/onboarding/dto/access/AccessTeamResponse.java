package io.probestack.onboarding.dto.access;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessTeamResponse {
    private String id;
    private String name;
    private String description;
    private String businessUnitId;
    private String projectId;
    private String applicationId;
    private long memberCount;
    private long applicationCount;
    private List<String> applicationIds;
    private String createdByEmail;
    private Instant createdAt;
}
