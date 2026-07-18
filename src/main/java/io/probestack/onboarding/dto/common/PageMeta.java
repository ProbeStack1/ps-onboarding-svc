package io.probestack.onboarding.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMeta {
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private String traceId;
    @Builder.Default
    private Instant timestamp = Instant.now();

    public static PageMeta simple(String traceId) {
        return PageMeta.builder().traceId(traceId).timestamp(Instant.now()).build();
    }

    public static PageMeta page(int page, int size, long totalElements, String traceId) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return PageMeta.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .traceId(traceId)
                .timestamp(Instant.now())
                .build();
    }
}
