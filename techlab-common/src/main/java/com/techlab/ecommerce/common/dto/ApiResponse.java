package com.techlab.ecommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.techlab.ecommerce.common.enums.CommonErrorMessage;
import com.techlab.ecommerce.common.exception.GenericException;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Unified envelope returned from every REST endpoint across all Techlab services.
 * Mirrors the shape used in the legacy `Response` class but with paged data extracted
 * cleanly and an explicit {@link ErrorPayload} for failures.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    public static final String SUCCESSFUL = "Successful";
    public static final String UNSUCCESSFUL = "UnSuccessful";

    private String serverDateTime = OffsetDateTime.now().toString();
    private int status = 200;
    private int code = 0;
    private String message = SUCCESSFUL;
    private String traceId;
    private T data;
    private PagedResult pagedResult;
    private ErrorPayload error;

    public ApiResponse() {
    }

    public ApiResponse(T data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        if (data instanceof Page<?> page) {
            response.data = (T) page.getContent();
            response.pagedResult = new PagedResult(
                    page.getNumber() + 1,
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages());
        } else {
            response.data = data;
        }
        return response;
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>();
    }

    public static ApiResponse<Void> error(GenericException ex, String traceId) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.status = ex.getStatus();
        response.code = ex.getCode();
        response.message = UNSUCCESSFUL;
        response.traceId = (traceId != null) ? traceId : UUID.randomUUID().toString();
        response.error = ErrorPayload.builder()
                .code(ex.getCode())
                .type(ex.getClass().getSimpleName())
                .message(ex.getMessage())
                .build();
        return response;
    }

    public static ApiResponse<Void> error(CommonErrorMessage err, String traceId) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.status = err.getHttpStatus();
        response.code = err.getCode();
        response.message = UNSUCCESSFUL;
        response.traceId = (traceId != null) ? traceId : UUID.randomUUID().toString();
        response.error = ErrorPayload.builder()
                .code(err.getCode())
                .type(err.name())
                .message(err.getMessage())
                .build();
        return response;
    }

    public static ApiResponse<Void> error(int status, int code, String type, String message, String traceId) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.status = status;
        response.code = code;
        response.message = UNSUCCESSFUL;
        response.traceId = (traceId != null) ? traceId : UUID.randomUUID().toString();
        response.error = ErrorPayload.builder()
                .code(code)
                .type(type)
                .message(message)
                .build();
        return response;
    }
}
