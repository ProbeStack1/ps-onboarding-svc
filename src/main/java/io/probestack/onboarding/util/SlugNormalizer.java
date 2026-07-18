package io.probestack.onboarding.util;

import org.springframework.util.StringUtils;

public final class SlugNormalizer {
    private SlugNormalizer() {
    }

    public static String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim().replaceAll("\\s+", "-").toUpperCase();
    }

    public static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
