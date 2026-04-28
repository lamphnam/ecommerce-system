package com.techlab.ecommerce.common.exception;

import com.techlab.ecommerce.common.enums.CommonErrorMessage;

public class ConflictException extends GenericException {

    public ConflictException(String message) {
        super(CommonErrorMessage.CONFLICT.getCode(),
              CommonErrorMessage.CONFLICT.getHttpStatus(),
              message);
    }

    public ConflictException(int code, String message) {
        super(code, CommonErrorMessage.CONFLICT.getHttpStatus(), message);
    }

    public ConflictException(CommonErrorMessage err) {
        super(err);
    }
}
