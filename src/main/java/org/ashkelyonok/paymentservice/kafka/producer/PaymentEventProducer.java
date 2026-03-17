package org.ashkelyonok.paymentservice.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.paymentservice.model.event.PaymentStatusEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${application.kafka.topic.payment-status}")
    private String paymentTopic;

    public void sendPaymentEvent(PaymentStatusEvent event) {
        log.info("Publishing PaymentStatusEvent for Order ID: {} to topic: {}", event.getOrderId(), paymentTopic);

        kafkaTemplate.send(paymentTopic, String.valueOf(event.getOrderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send payment event for Order ID {}. Reason: {}", event.getOrderId(), ex.getMessage());
                    } else {
                        log.debug("Successfully sent payment event for Order ID {}", event.getOrderId());
                    }
                });
    }
}