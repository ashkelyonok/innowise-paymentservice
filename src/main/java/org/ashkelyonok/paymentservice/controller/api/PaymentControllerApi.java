package org.ashkelyonok.paymentservice.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ashkelyonok.paymentservice.model.dto.error.ErrorResponseDto;
import org.ashkelyonok.paymentservice.model.dto.error.ValidationErrorResponseDto;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentCreateDto;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentFilterDto;
import org.ashkelyonok.paymentservice.model.dto.response.PaymentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Tag(name = "Payments", description = "Payment Processing and Management")
public interface PaymentControllerApi {

    @Operation(summary = "Create new payment", description = "Processes payment for an order.")
    @ApiResponse(responseCode = "201", description = "Payment created successfully", content = @Content(schema = @Schema(implementation = PaymentResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid payment data", content = @Content(schema = @Schema(implementation = ValidationErrorResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    ResponseEntity<PaymentResponseDto> createPayment(PaymentCreateDto request);

    @Operation(summary = "Search payments", description = "Advanced search with filters: orderId, userId, status. For status filter ADMIN role is required.")
    @ApiResponse(responseCode = "200", description = "Payments found")
    ResponseEntity<Page<PaymentResponseDto>> searchPayments(PaymentFilterDto filterDto, Pageable pageable);

    @Operation(summary = "Get total sum for user", description = "Calculates total payment amount for a user within date range.")
    @ApiResponse(responseCode = "200", description = "Total sum calculated")
    ResponseEntity<BigDecimal> getTotalSumForUser(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    @Operation(summary = "Get total sum for all users", description = "Calculates total payment amount for all users within date range. Admin only.")
    @ApiResponse(responseCode = "200", description = "Total sum calculated")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ResponseEntity<BigDecimal> getTotalSumForAllUsers(LocalDateTime startDate, LocalDateTime endDate);
}