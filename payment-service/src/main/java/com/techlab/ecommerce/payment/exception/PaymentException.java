package com.techlab.ecommerce.payment.exception;

import com.techlab.ecommerce.common.exception.GenericException;
import com.techlab.ecommerce.payment.enums.PaymentErrorMessage;

/**
 * Payment Service-specific business exception mapped by the common global
 * exception handler to the uniform ApiResponse error shape.
 */
public class PaymentException extends GenericException {

    public PaymentException(PaymentErrorMessage err) {
        super(err.getCode(), err.getHttpStatus(), err.getMessage());
    }

    public PaymentException(PaymentErrorMessage err, String overrideMessage) {
        super(err.getCode(), err.getHttpStatus(), overrideMessage);
    }
}
