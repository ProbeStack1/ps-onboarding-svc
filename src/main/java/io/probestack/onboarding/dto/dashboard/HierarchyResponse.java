package io.probestack.onboarding.dto.dashboard;

import io.probestack.onboarding.dto.businessunit.BusinessUnitTreeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HierarchyResponse {
    private List<BusinessUnitTreeResponse> businessUnits;
}
