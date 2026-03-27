package org.ashkelyonok.paymentservice.model.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event emitted regarding the payment outcome of a specific order")
public class PaymentStatusEvent {

    @Schema(description = "The Order ID this payment update belongs to",
            example = "1001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @Schema(description = "The status of the payment transaction",
            example = "SUCCESS",
            allowableValues = {"SUCCESS", "FAILED", "PENDING"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "Internal Payment ID (from MongoDB)",
            example = "64b1f2e8c9e77b4d1c3a1234")
    private String paymentId;

    @Schema(description = "Timestamp when the payment was processed",
            example = "2026-03-12T09:41:00")
    private LocalDateTime timestamp;
}