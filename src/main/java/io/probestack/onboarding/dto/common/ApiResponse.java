package io.probestack.onboarding.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String status;
    private String code;
    private String message;
    private T data;
    private List<ApiError> errors;
    private PageMeta meta;

    public static <T> ApiResponse<T> success(String code, String message, T data, PageMeta meta) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .code(code)
                .message(message)
                .data(data)
                .meta(meta)
                .build();
    }

    public static <T> ApiResponse<T> failure(String code, String message, List<ApiError> errors, PageMeta meta) {
        return ApiResponse.<T>builder()
                .status("FAILED")
                .code(code)
                .message(message)
                .errors(errors)
                .meta(meta)
                .build();
    }
}
