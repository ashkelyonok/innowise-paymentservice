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

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByOrderId(Long orderId);

    Page<Payment> findAllByUserId(Long userId, Pageable pageable);

    Page<Payment> findAllByStatus(PaymentStatus status, Pageable pageable);

    @Aggregation(pipeline = {
            "{ '$match': { 'user_id': ?0, 'timestamp': { $gte: ?1, $lte: ?2 }, 'status': 'SUCCESS' } }",
            "{ '$group': { '_id': null, 'total': { $sum: '$payment_amount' } } }"
    })
    Optional<PaymentSumView> calculateTotalSumForUser(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    @Aggregation(pipeline = {
            "{ '$match': { 'timestamp': { $gte: ?0, $lte: ?1 }, 'status': 'SUCCESS' } }",
            "{ '$group': { '_id': null, 'total': { $sum: '$payment_amount' } } }"
    })
    Optional<PaymentSumView> calculateTotalSumForAllUsers(LocalDateTime startDate, LocalDateTime endDate);
}