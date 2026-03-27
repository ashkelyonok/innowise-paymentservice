package org.ashkelyonok.paymentservice.exception;

import java.io.Serial;

public class KafkaPublishException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 7003046421775212681L;

  public KafkaPublishException() {
    super("Failed to publish payment event to Kafka");
  }

  public KafkaPublishException(String message) {
    super(message);
  }

  public KafkaPublishException(String message, Throwable cause) {
    super(message, cause);
  }
}