package org.ashkelyonok.paymentservice.model.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event received from Kafka when a new order is created in the Order Service")
public class OrderCreatedEvent {

    @Schema(description = "The ID of the newly created order",
            example = "1001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @Schema(description = "The ID of the user who owns the order",
            example = "500",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "The total amount to be paid for the order",
            example = "59.98",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalAmount;

    @Schema(description = "Timestamp when the order was created",
            example = "2026-03-12T09:41:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;
}