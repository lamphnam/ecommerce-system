package com.techlab.ecommerce.common.exception;

import com.techlab.ecommerce.common.enums.CommonErrorMessage;
import lombok.Getter;

/**
 * Root of all business exceptions. Carries a business {@code code} and an HTTP {@code status}
 * so the {@link com.techlab.ecommerce.common.exception.handler.GlobalExceptionHandler}
 * can build a uniform {@link com.techlab.ecommerce.common.dto.ApiResponse}.
 */
@Getter
public abstract class GenericException extends RuntimeException {

    protected final int code;
    protected final int status;

    protected GenericException(int code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    protected GenericException(int code, int status, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    protected GenericException(CommonErrorMessage err) {
        super(err.getMessage());
        this.code = err.getCode();
        this.status = err.getHttpStatus();
    }

    protected GenericException(CommonErrorMessage err, String overrideMessage) {
        super(overrideMessage);
        this.code = err.getCode();
        this.status = err.getHttpStatus();
    }
}
