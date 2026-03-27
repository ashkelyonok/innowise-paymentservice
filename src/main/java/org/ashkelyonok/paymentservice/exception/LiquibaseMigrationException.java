package org.ashkelyonok.paymentservice.exception;

import java.io.Serial;

public class LiquibaseMigrationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 8553621877350983197L;

    public LiquibaseMigrationException() {
        super("Failed to apply database migrations. Please check your database connection and try again.");
    }

    public LiquibaseMigrationException(String message) {
        super(message);
    }

    public LiquibaseMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}