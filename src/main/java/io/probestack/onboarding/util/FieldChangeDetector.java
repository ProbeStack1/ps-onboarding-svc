package io.probestack.onboarding.util;

import io.probestack.onboarding.model.FieldChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FieldChangeDetector {
    private FieldChangeDetector() {
    }

    public static List<FieldChange> diff(Map<String, Object> before, Map<String, Object> after) {
        List<FieldChange> changes = new ArrayList<>();
        after.forEach((field, newValue) -> {
            Object oldValue = before.get(field);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(FieldChange.builder()
                        .field(field)
                        .oldValue(oldValue == null ? null : String.valueOf(oldValue))
                        .newValue(newValue == null ? null : String.valueOf(newValue))
                        .build());
            }
        });
        return changes;
    }
}
