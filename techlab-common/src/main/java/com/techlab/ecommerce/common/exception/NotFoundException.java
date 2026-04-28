package com.techlab.ecommerce.common.exception;

import com.techlab.ecommerce.common.enums.CommonErrorMessage;

public class NotFoundException extends GenericException {

    public NotFoundException(String message) {
        super(CommonErrorMessage.NOT_FOUND.getCode(),
              CommonErrorMessage.NOT_FOUND.getHttpStatus(),
              message);
    }

    public NotFoundException(int code, String message) {
        super(code, CommonErrorMessage.NOT_FOUND.getHttpStatus(), message);
    }

    public NotFoundException(CommonErrorMessage err) {
        super(err);
    }
}
