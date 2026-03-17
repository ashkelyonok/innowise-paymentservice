package org.ashkelyonok.paymentservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ashkelyonok.paymentservice.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for payment details")
public class PaymentResponseDto {

    @Schema(description = "Unique payment identifier (MongoDB UUID)",
            example = "64b1f2e8c9e77b4d1c3a1234")
    private String id;

    @Schema(description = "Associated order ID",
            example = "1001")
    private Long orderId;

    @Schema(description = "Associated user ID",
            example = "500")
    private Long userId;

    @Schema(description = "Current status of the payment",
            example = "SUCCESS")
    private PaymentStatus status;

    @Schema(description = "Payment creation timestamp",
            example = "2026-03-12T09:41:00",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime timestamp;

    @Schema(description = "Total amount paid",
            example = "59.98")
    private BigDecimal paymentAmount;
}