package com.techlab.ecommerce.common.exception;

import com.techlab.ecommerce.common.enums.CommonErrorMessage;

public class BadRequestException extends GenericException {

    public BadRequestException(String message) {
        super(CommonErrorMessage.BAD_REQUEST.getCode(),
              CommonErrorMessage.BAD_REQUEST.getHttpStatus(),
              message);
    }

    public BadRequestException(int code, String message) {
        super(code, CommonErrorMessage.BAD_REQUEST.getHttpStatus(), message);
    }

    public BadRequestException(CommonErrorMessage err) {
        super(err);
    }
}
