package org.ashkelyonok.paymentservice.repository;

import org.ashkelyonok.paymentservice.model.dto.response.PaymentSumView;
import org.ashkelyonok.paymentservice.model.entity.Payment;
import org.ashkelyonok.paymentservice.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for managing Payment entities in MongoDB.
 * Provides CRUD operations and custom query methods for payment data access.
 */
@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    /**
     * Finds a payment by its associated order ID.
     *
     * @param orderId the unique identifier of the order
     * @return an Optional containing the payment if found, or empty if not found
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Retrieves a paginated list of payments for a specific user.
     *
     * @param userId the unique identifier of the user
     * @param pageable pagination information (page number, size, sorting)
     * @return a page of payments belonging to the specified user
     */
    Page<Payment> findAllByUserId(Long userId, Pageable pageable);

    /**
     * Retrieves a paginated list of payments with a specific status.
     *
     * @param status the payment status to filter by (SUCCESS, FAILED, PENDING)
     * @param pageable pagination information (page number, size, sorting)
     * @return a page of payments with the specified status
     */
    Page<Payment> findAllByStatus(PaymentStatus status, Pageable pageable);

    /**
     * Calculates the total sum of successful payments for a specific user within a date range.
     * Uses MongoDB aggregation pipeline to sum payment amounts for SUCCESS status payments.
     *
     * @param userId the unique identifier of the user
     * @param startDate the beginning of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return an Optional containing the PaymentSumView with the total sum, or empty if no payments found
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'user_id': ?0, 'timestamp': { $gte: ?1, $lte: ?2 }, 'status': 'SUCCESS' } }",
            "{ '$group': { '_id': null, 'total': { $sum: '$payment_amount' } } }"
    })
    Optional<PaymentSumView> calculateTotalSumForUser(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Calculates the total sum of all successful payments across all users within a date range.
     * Uses MongoDB aggregation pipeline to sum payment amounts for SUCCESS status payments.
     *
     * @param startDate the beginning of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return an Optional containing the PaymentSumView with the total sum, or empty if no payments found
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'timestamp': { $gte: ?0, $lte: ?1 }, 'status': 'SUCCESS' } }",
            "{ '$group': { '_id': null, 'total': { $sum: '$payment_amount' } } }"
    })
    Optional<PaymentSumView> calculateTotalSumForAllUsers(LocalDateTime startDate, LocalDateTime endDate);
}