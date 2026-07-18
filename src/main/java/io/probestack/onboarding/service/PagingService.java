package io.probestack.onboarding.service;

import io.probestack.onboarding.dto.common.PagedResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PagingService {
    public <T> PagedResult<T> page(List<T> items, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        int from = Math.min(safePage * safeSize, items.size());
        int to = Math.min(from + safeSize, items.size());
        return PagedResult.<T>builder()
                .items(items.subList(from, to))
                .totalElements(items.size())
                .build();
    }
}
