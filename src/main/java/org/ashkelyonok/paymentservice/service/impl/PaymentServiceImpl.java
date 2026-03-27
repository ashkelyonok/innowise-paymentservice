package org.ashkelyonok.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.paymentservice.client.RandomApiClient;
import org.ashkelyonok.paymentservice.exception.InvalidPaymentOperationException;
import org.ashkelyonok.paymentservice.exception.PaymentNotFoundException;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentFilterDto;
import org.ashkelyonok.paymentservice.model.event.PaymentStatusEvent;
import org.ashkelyonok.paymentservice.kafka.producer.PaymentEventProducer;
import org.ashkelyonok.paymentservice.mapper.PaymentMapper;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentCreateDto;
import org.ashkelyonok.paymentservice.model.dto.response.PaymentResponseDto;
import org.ashkelyonok.paymentservice.model.dto.response.PaymentSumView;
import org.ashkelyonok.paymentservice.model.entity.Payment;
import org.ashkelyonok.paymentservice.model.enums.PaymentStatus;
import org.ashkelyonok.paymentservice.repository.PaymentRepository;
import org.ashkelyonok.paymentservice.security.SecurityUtil;
import org.ashkelyonok.paymentservice.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomApiClient randomApiClient;
    private final PaymentEventProducer paymentEventProducer;
    private final SecurityUtil securityUtil;

    @Override
    public boolean initializePayment(PaymentCreateDto request) {
        if (paymentRepository.findByOrderId(request.getOrderId()).isPresent()) {
            log.warn("Payment for Order ID {} already initialized. Skipping.", request.getOrderId());
            return false;
        }

        Payment payment = paymentMapper.toEntity(request);
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        return true;
    }

    @Override
    public PaymentResponseDto processPayment(PaymentCreateDto request) {
        log.info("Processing payment for Order ID: {}", request.getOrderId());

        securityUtil.checkOwnership(request.getUserId());

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseGet(() -> paymentMapper.toEntity(request));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidPaymentOperationException("Payment for Order ID " + request.getOrderId() + " is already SUCCESSFUL.");
        }

        int randomNumber = fetchRandomNumber();
        payment.setStatus(determinePaymentStatus(randomNumber));

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment saved with ID: {} and Status: {}", savedPayment.getId(), savedPayment.getStatus());

        publishPaymentEvent(savedPayment);

        return paymentMapper.toResponseDto(savedPayment);
    }

    @Override
    public Page<PaymentResponseDto> searchPayments(PaymentFilterDto filter, Pageable pageable) {
        if (filter.getOrderId() != null) {
            Payment payment = paymentRepository.findByOrderId(filter.getOrderId())
                    .orElseThrow(() -> new PaymentNotFoundException(filter.getOrderId()));

            securityUtil.checkOwnership(payment.getUserId());
            return new PageImpl<>(List.of(paymentMapper.toResponseDto(payment)), pageable, 1);
        }
        else if (filter.getUserId() != null) {
            securityUtil.checkOwnership(filter.getUserId());
            return paymentRepository.findAllByUserId(filter.getUserId(), pageable)
                    .map(paymentMapper::toResponseDto);
        }
        else if (filter.getStatus() != null) {
            if (!securityUtil.isAdmin()) {
                throw new AccessDeniedException("Only administrators can filter globally by payment status.");
            }
            return paymentRepository.findAllByStatus(filter.getStatus(), pageable)
                    .map(paymentMapper::toResponseDto);
        }

        if (securityUtil.isAdmin()) {
            return paymentRepository.findAll(pageable).map(paymentMapper::toResponseDto);
        } else {
            Long currentUserId = securityUtil.getAuthenticatedUserId();
            return paymentRepository.findAllByUserId(currentUserId, pageable).map(paymentMapper::toResponseDto);
        }
    }

    @Override
    public BigDecimal getTotalSumForUser(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        securityUtil.checkOwnership(userId);
        return paymentRepository.calculateTotalSumForUser(userId, startDate, endDate)
                .map(PaymentSumView::total)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getTotalSumForAllUsers(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.calculateTotalSumForAllUsers(startDate, endDate)
                .map(PaymentSumView::total)
                .orElse(BigDecimal.ZERO);
    }

    private int fetchRandomNumber() {
        try {
            String response = randomApiClient.fetchRandomNumberString();
            return Integer.parseInt(response.trim());
        } catch (Exception e) {
            log.error("Failed to parse random number from API. Using fallback.", e);
            return 1;
        }
    }

    private PaymentStatus determinePaymentStatus(int number) {
        return (number % 2 == 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }

    private void publishPaymentEvent(Payment payment) {
        PaymentStatusEvent event = paymentMapper.toEvent(payment);
        paymentEventProducer.sendPaymentEvent(event);
    }
}