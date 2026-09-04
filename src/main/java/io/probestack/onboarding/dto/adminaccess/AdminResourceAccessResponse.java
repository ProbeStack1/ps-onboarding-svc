package io.probestack.onboarding.dto.adminaccess;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResourceAccessResponse {
    private AdminResourceType resourceType;
    private String resourceId;
    private String resourceName;
    private String status;
    private String businessUnitId;
    private String projectId;
    private long userCount;
    private List<ResourcePrincipalAccessResponse> users;
}
