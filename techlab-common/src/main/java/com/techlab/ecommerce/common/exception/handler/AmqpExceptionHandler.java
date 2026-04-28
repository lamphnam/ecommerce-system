package com.techlab.ecommerce.common.exception.handler;

import com.techlab.ecommerce.common.dto.ApiResponse;
import com.techlab.ecommerce.common.enums.CommonErrorMessage;
import com.techlab.ecommerce.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps {@link AmqpException} to HTTP 503. Only registered when Spring AMQP is on
 * the classpath, so the API Gateway (which has no AMQP dependency) doesn't break.
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnClass(AmqpException.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AmqpExceptionHandler {

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiResponse<Void>> handleAmqp(AmqpException ex, HttpServletRequest request) {
        String traceId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ID_ATTRIBUTE);
        log.error("[{}] AMQP failure: {}", traceId, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(CommonErrorMessage.SERVICE_UNAVAILABLE, traceId));
    }
}
