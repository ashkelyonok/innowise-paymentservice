package org.ashkelyonok.paymentservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for creating a new payment")
public class PaymentCreateDto {

    @Schema(description = "ID of the order being paid for",
            example = "1001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Order ID cannot be null")
    private Long orderId;

    @Schema(description = "ID of the user making the payment",
            example = "500",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @Schema(description = "Amount to be paid",
            example = "59.98",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Payment amount cannot be null")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal paymentAmount;
}