package org.ashkelyonok.paymentservice.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.paymentservice.exception.KafkaPublishException;
import org.ashkelyonok.paymentservice.model.event.PaymentStatusEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${application.kafka.topic.payment-status}")
    private String paymentTopic;

    public void sendPaymentEvent(PaymentStatusEvent event) {
        log.info("Publishing PaymentStatusEvent for Order ID: {} to topic: {}", event.getOrderId(), paymentTopic);

        try {
            kafkaTemplate.send(paymentTopic, String.valueOf(event.getOrderId()), event)
                    .get(5, TimeUnit.SECONDS);
            log.debug("Successfully sent payment event for Order ID {}", event.getOrderId());
        } catch (KafkaException e) {
            log.error("Kafka error while sending payment event for Order ID: {}", event.getOrderId(), e);
            throw new KafkaPublishException(
                    String.format("Kafka error while publishing payment event for Order ID %d", event.getOrderId()),
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while sending payment event for Order ID: {}", event.getOrderId(), e);
            throw new KafkaPublishException(
                    String.format("Failed to publish payment event for Order ID %d due to thread interruption", event.getOrderId()),
                    e
            );
        } catch (ExecutionException e) {
            log.error("Kafka execution failed while sending payment event for Order ID: {}", event.getOrderId(), e);
            throw new KafkaPublishException(
                    String.format("Failed to publish payment event for Order ID %d - Kafka operation failed", event.getOrderId()),
                    e
            );
        } catch (TimeoutException e) {
            log.error("Timeout while waiting for Kafka to send payment event for Order ID: {}", event.getOrderId(), e);
            throw new KafkaPublishException(
                    String.format("Timeout waiting for Kafka response for Order ID %d after 5 seconds", event.getOrderId()),
                    e
            );
        }
    }
}