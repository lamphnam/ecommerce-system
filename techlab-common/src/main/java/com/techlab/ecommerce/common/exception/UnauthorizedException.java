package com.techlab.ecommerce.common.exception;

import com.techlab.ecommerce.common.enums.CommonErrorMessage;

public class UnauthorizedException extends GenericException {

    public UnauthorizedException(String message) {
        super(CommonErrorMessage.UNAUTHORIZED.getCode(),
              CommonErrorMessage.UNAUTHORIZED.getHttpStatus(),
              message);
    }

    public UnauthorizedException(CommonErrorMessage err) {
        super(err);
    }
}
