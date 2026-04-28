package com.techlab.ecommerce.common.exception.handler;

import com.techlab.ecommerce.common.dto.ApiResponse;
import com.techlab.ecommerce.common.dto.ErrorPayload;
import com.techlab.ecommerce.common.enums.CommonErrorMessage;
import com.techlab.ecommerce.common.exception.GenericException;
import com.techlab.ecommerce.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * One handler to rule them all. Imported by every service via component scan of
 * {@code com.techlab.ecommerce.common}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GenericException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(GenericException ex, HttpServletRequest request) {
        log.warn("[{}] {} -> {}", traceId(request), ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex, traceId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<ErrorPayload.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorPayload.FieldViolation.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .rejectedValue(fe.getRejectedValue())
                        .build())
                .toList();
        ApiResponse<Void> body = ApiResponse.error(CommonErrorMessage.VALIDATION_FAILED, traceId(request));
        body.getError().setViolations(violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex,
                                                                       HttpServletRequest request) {
        List<ErrorPayload.FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> ErrorPayload.FieldViolation.builder()
                        .field(v.getPropertyPath().toString())
                        .message(v.getMessage())
                        .rejectedValue(v.getInvalidValue())
                        .build())
                .toList();
        ApiResponse<Void> body = ApiResponse.error(CommonErrorMessage.VALIDATION_FAILED, traceId(request));
        body.getError().setViolations(violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.debug("[{}] bad request: {}", traceId(request), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorMessage.BAD_REQUEST, traceId(request)));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(CommonErrorMessage.NOT_FOUND, traceId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex, HttpServletRequest request) {
        log.error("[{}] Unhandled exception", traceId(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(CommonErrorMessage.INTERNAL_ERROR, traceId(request)));
    }

    private static String traceId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ID_ATTRIBUTE);
        if (attr instanceof String s && !s.isBlank()) {
            return s;
        }
        return request.getHeader(CorrelationIdFilter.HEADER);
    }
}
