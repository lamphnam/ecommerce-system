package com.techlab.ecommerce.gateway.exception;

import com.techlab.ecommerce.common.exception.GenericException;
import com.techlab.ecommerce.gateway.enums.GatewayErrorMessage;

/**
 * API Gateway-specific business exception mapped by the common global exception
 * handler to the uniform ApiResponse error shape.
 */
public class GatewayException extends GenericException {

    public GatewayException(GatewayErrorMessage err) {
        super(err.getCode(), err.getHttpStatus(), err.getMessage());
    }

    public GatewayException(GatewayErrorMessage err, String overrideMessage) {
        super(err.getCode(), err.getHttpStatus(), overrideMessage);
    }
}
