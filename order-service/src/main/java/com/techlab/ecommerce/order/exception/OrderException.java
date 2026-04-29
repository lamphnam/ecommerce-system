package com.techlab.ecommerce.order.exception;

import com.techlab.ecommerce.common.exception.GenericException;
import com.techlab.ecommerce.order.enums.OrderErrorMessage;

/**
 * Order Service-specific business exception. Carries an
 * {@link OrderErrorMessage} so the {@code GlobalExceptionHandler} in
 * {@code techlab-common} maps it to a uniform {@code ApiResponse} with the
 * correct HTTP status and code.
 */
public class OrderException extends GenericException {

    public OrderException(OrderErrorMessage err) {
        super(err.getCode(), err.getHttpStatus(), err.getMessage());
    }

    public OrderException(OrderErrorMessage err, String overrideMessage) {
        super(err.getCode(), err.getHttpStatus(), overrideMessage);
    }
}
