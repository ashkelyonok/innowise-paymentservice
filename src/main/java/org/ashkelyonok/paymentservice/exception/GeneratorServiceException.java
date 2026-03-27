package org.ashkelyonok.paymentservice.exception;

import java.io.Serial;

public class GeneratorServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -142937722741988174L;

    public GeneratorServiceException() {
        super("Generator service error occurred");
    }

    public GeneratorServiceException(String message) {
        super(message);
    }

    public GeneratorServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}