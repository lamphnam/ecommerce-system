package com.techlab.ecommerce.analytics.exception;

import com.techlab.ecommerce.analytics.enums.AnalyticsErrorMessage;
import com.techlab.ecommerce.common.exception.GenericException;

/**
 * Analytics Service-specific business exception mapped by the common global
 * exception handler to the uniform ApiResponse error shape.
 */
public class AnalyticsException extends GenericException {

    public AnalyticsException(AnalyticsErrorMessage err) {
        super(err.getCode(), err.getHttpStatus(), err.getMessage());
    }

    public AnalyticsException(AnalyticsErrorMessage err, String overrideMessage) {
        super(err.getCode(), err.getHttpStatus(), overrideMessage);
    }
}
