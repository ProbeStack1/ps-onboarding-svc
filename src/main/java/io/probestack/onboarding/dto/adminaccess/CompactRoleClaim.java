package io.probestack.onboarding.dto.adminaccess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompactRoleClaim {
    private String role;
    @JsonProperty("scope_type")
    private String scopeType;
    @JsonProperty("scope_id")
    private String scopeId;
    private List<String> permissions;
}
