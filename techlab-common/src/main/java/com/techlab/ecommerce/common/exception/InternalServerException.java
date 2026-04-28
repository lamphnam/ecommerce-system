package com.techlab.ecommerce.common.exception;

import com.techlab.ecommerce.common.enums.CommonErrorMessage;

public class InternalServerException extends GenericException {

    public InternalServerException(String message) {
        super(CommonErrorMessage.INTERNAL_ERROR.getCode(),
              CommonErrorMessage.INTERNAL_ERROR.getHttpStatus(),
              message);
    }

    public InternalServerException(String message, Throwable cause) {
        super(CommonErrorMessage.INTERNAL_ERROR.getCode(),
              CommonErrorMessage.INTERNAL_ERROR.getHttpStatus(),
              message,
              cause);
    }
}
