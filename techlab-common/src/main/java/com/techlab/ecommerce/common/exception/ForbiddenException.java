package com.techlab.ecommerce.common.exception;

import com.techlab.ecommerce.common.enums.CommonErrorMessage;

public class ForbiddenException extends GenericException {

    public ForbiddenException(String message) {
        super(CommonErrorMessage.FORBIDDEN.getCode(),
              CommonErrorMessage.FORBIDDEN.getHttpStatus(),
              message);
    }

    public ForbiddenException(CommonErrorMessage err) {
        super(err);
    }
}
