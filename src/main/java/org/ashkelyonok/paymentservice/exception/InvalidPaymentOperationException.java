package org.ashkelyonok.paymentservice.exception;

import java.io.Serial;

public class InvalidPaymentOperationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -131064236862521143L;

    public InvalidPaymentOperationException() {
        super("Invalid payment operation");
    }

    public InvalidPaymentOperationException(String message) {
        super(message);
    }

    public InvalidPaymentOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}