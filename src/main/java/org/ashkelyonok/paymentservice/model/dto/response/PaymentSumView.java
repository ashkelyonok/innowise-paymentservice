package org.ashkelyonok.paymentservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "View containing the total sum of payments")
public record PaymentSumView(
        @Schema(description = "Total payment amount",
                example = "1250.75",
                accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal total
) {}