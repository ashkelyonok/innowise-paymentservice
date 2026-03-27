package org.ashkelyonok.paymentservice.exception;

import java.io.Serial;

public class PaymentNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -898829952009301763L;

    public PaymentNotFoundException() {
        super("Payment not found");
    }

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(Long orderId) {
        super("Payment not found for Order ID: " + orderId);
    }

    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}