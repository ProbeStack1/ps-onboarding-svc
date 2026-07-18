package io.probestack.onboarding.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectorOption {
    private String id;
    private String label;
    private String code;
    private String status;
}
