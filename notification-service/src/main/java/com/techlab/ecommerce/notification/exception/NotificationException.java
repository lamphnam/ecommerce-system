package com.techlab.ecommerce.notification.exception;

import com.techlab.ecommerce.common.exception.GenericException;
import com.techlab.ecommerce.notification.enums.NotificationErrorMessage;

/**
 * Notification Service-specific business exception mapped by the common global
 * exception handler to the uniform ApiResponse error shape.
 */
public class NotificationException extends GenericException {

    public NotificationException(NotificationErrorMessage err) {
        super(err.getCode(), err.getHttpStatus(), err.getMessage());
    }

    public NotificationException(NotificationErrorMessage err, String overrideMessage) {
        super(err.getCode(), err.getHttpStatus(), overrideMessage);
    }
}
