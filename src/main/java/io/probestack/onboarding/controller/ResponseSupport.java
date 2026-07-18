package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.dto.common.PageMeta;
import io.probestack.onboarding.dto.common.PagedResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class ResponseSupport {
    protected <T> ResponseEntity<ApiResponse<T>> ok(String message, T data, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("ONBOARDING_200", message, data, PageMeta.simple(traceId(request))));
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(String message, T data, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("ONBOARDING_201", message, data, PageMeta.simple(traceId(request))));
    }

    protected <T> ResponseEntity<ApiResponse<java.util.List<T>>> page(String message, PagedResult<T> result, int page, int size, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("ONBOARDING_200", message, result.getItems(), PageMeta.page(Math.max(page, 0), size <= 0 ? 20 : Math.min(size, 200), result.getTotalElements(), traceId(request))));
    }

    protected ResponseEntity<ApiResponse<Void>> noData(String message, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("ONBOARDING_200", message, null, PageMeta.simple(traceId(request))));
    }

    private String traceId(HttpServletRequest request) {
        return request == null ? null : request.getHeader("X-Trace-Id");
    }
}
