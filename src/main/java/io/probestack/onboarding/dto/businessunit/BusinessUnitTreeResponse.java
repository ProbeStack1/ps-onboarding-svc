package io.probestack.onboarding.dto.businessunit;

import io.probestack.onboarding.dto.project.ProjectTreeNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessUnitTreeResponse {
    private BusinessUnitResponse businessUnit;
    private long projectCount;
    private long applicationCount;
    private List<ProjectTreeNode> projects;
}
