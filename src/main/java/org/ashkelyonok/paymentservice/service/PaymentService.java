package org.ashkelyonok.paymentservice.service;

import org.ashkelyonok.paymentservice.model.dto.request.PaymentCreateDto;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentFilterDto;
import org.ashkelyonok.paymentservice.model.dto.response.PaymentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service interface for managing payments.
 */
public interface PaymentService {

    /**
     * Initializes a pending payment from order creation event.
     *
     * @param request payment creation data
     * @return true if initialized, false if already exists
     */
    boolean initializePayment(PaymentCreateDto request);

    /**
     * Processes payment and determines final status via external API.
     *
     * @param request payment processing data
     * @return processed payment details
     */
    PaymentResponseDto processPayment(PaymentCreateDto request);

    /**
     * Searches payments by order ID, user ID, or status (admin-only for status).
     *
     * @param filter search criteria
     * @param pageable pagination parameters
     * @return page of matching payments
     */
    Page<PaymentResponseDto> searchPayments(PaymentFilterDto filter, Pageable pageable);

    /**
     * Calculates total sum of successful payments for a user within date range.
     *
     * @param userId user identifier
     * @param startDate range start
     * @param endDate range end
     * @return total sum or zero if none
     */
    BigDecimal getTotalSumForUser(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Calculates total sum of successful payments for all users within date range.
     * Admin only.
     *
     * @param startDate range start
     * @param endDate range end
     * @return total sum or zero if none
     */
    BigDecimal getTotalSumForAllUsers(LocalDateTime startDate, LocalDateTime endDate);
}