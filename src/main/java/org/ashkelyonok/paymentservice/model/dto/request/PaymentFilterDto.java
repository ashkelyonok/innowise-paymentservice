package org.ashkelyonok.paymentservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ashkelyonok.paymentservice.model.enums.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for filtering payments in search queries. Provide one of the fields to filter.")
public class PaymentFilterDto {

    @Schema(description = "Filter by specific Order ID",
            example = "1001",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long orderId;

    @Schema(description = "Filter by specific User ID",
            example = "500",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long userId;

    @Schema(description = "Filter globally by Payment Status (Admin Only)",
            example = "SUCCESS",
            allowableValues = {"PENDING", "SUCCESS", "FAILED", "REFUNDED"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private PaymentStatus status;
}