package org.ashkelyonok.paymentservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.paymentservice.model.event.OrderCreatedEvent;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentCreateDto;
import org.ashkelyonok.paymentservice.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${application.kafka.topic.order-created:order-created-topic}",
            groupId = "${application.kafka.consumer-group-id:payment-service-group}"
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for Order ID: {}", event.getOrderId());

        PaymentCreateDto dto = PaymentCreateDto.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .paymentAmount(event.getTotalAmount())
                .build();

        boolean isInitialized = paymentService.initializePayment(dto);

        if (isInitialized) {
            log.info("Successfully initialized pending payment for Order ID: {}", event.getOrderId());
        } else {
            log.info("Ignored OrderCreatedEvent for Order ID: {} (Already exists)", event.getOrderId());
        }
    }
}