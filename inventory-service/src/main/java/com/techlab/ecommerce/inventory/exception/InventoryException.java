package com.techlab.ecommerce.inventory.exception;

import com.techlab.ecommerce.common.exception.GenericException;
import com.techlab.ecommerce.inventory.enums.InventoryErrorMessage;

/**
 * Inventory Service-specific business exception mapped by the common global
 * exception handler to the uniform ApiResponse error shape.
 */
public class InventoryException extends GenericException {

    public InventoryException(InventoryErrorMessage err) {
        super(err.getCode(), err.getHttpStatus(), err.getMessage());
    }

    public InventoryException(InventoryErrorMessage err, String overrideMessage) {
        super(err.getCode(), err.getHttpStatus(), overrideMessage);
    }
}
