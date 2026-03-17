package org.ashkelyonok.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ashkelyonok.paymentservice.controller.api.PaymentControllerApi;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentCreateDto;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentFilterDto;
import org.ashkelyonok.paymentservice.model.dto.response.PaymentResponseDto;
import org.ashkelyonok.paymentservice.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController implements PaymentControllerApi {

    private final PaymentService paymentService;

    @Override
    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody PaymentCreateDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processPayment(request));
    }

    @Override
    @GetMapping
    public ResponseEntity<Page<PaymentResponseDto>> searchPayments(
            @ModelAttribute PaymentFilterDto filterDto,
            Pageable pageable) {
        return ResponseEntity.ok(paymentService.searchPayments(filterDto, pageable));
    }

    @Override
    @GetMapping("/users/{userId}/sum")
    public ResponseEntity<BigDecimal> getTotalSumForUser(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(paymentService.getTotalSumForUser(userId, startDate, endDate));
    }

    @Override
    @GetMapping("/sum")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getTotalSumForAllUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(paymentService.getTotalSumForAllUsers(startDate, endDate));
    }
}