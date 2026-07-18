package io.probestack.onboarding.dto.project;

import io.probestack.onboarding.dto.application.ApplicationSummaryResponse;
import io.probestack.onboarding.model.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTreeNode {
    private String id;
    private String name;
    private String code;
    private String ownerName;
    private LocalDate expectedGoLiveDate;
    private ProjectStatus status;
    private long applicationCount;
    private List<ApplicationSummaryResponse> applications;
}
