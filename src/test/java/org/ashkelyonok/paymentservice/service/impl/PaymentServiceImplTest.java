package org.ashkelyonok.paymentservice.service.impl;

import org.ashkelyonok.paymentservice.client.RandomApiClient;
import org.ashkelyonok.paymentservice.exception.InvalidPaymentOperationException;
import org.ashkelyonok.paymentservice.kafka.producer.PaymentEventProducer;
import org.ashkelyonok.paymentservice.mapper.PaymentMapper;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentCreateDto;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentFilterDto;
import org.ashkelyonok.paymentservice.model.dto.response.PaymentResponseDto;
import org.ashkelyonok.paymentservice.model.dto.response.PaymentSumView;
import org.ashkelyonok.paymentservice.model.entity.Payment;
import org.ashkelyonok.paymentservice.model.enums.PaymentStatus;
import org.ashkelyonok.paymentservice.model.event.PaymentStatusEvent;
import org.ashkelyonok.paymentservice.repository.PaymentRepository;
import org.ashkelyonok.paymentservice.security.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private RandomApiClient randomApiClient;
    @Mock
    private PaymentEventProducer paymentEventProducer;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Initialize Payment: Should return false when payment already exists for Order ID")
    void initializePayment_ShouldReturnFalse_WhenPaymentExists() {
        PaymentCreateDto request = new PaymentCreateDto();
        request.setOrderId(100L);

        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(new Payment()));

        boolean result = paymentService.initializePayment(request);

        assertThat(result).isFalse();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Initialize Payment: Should save PENDING payment and return true when data is valid")
    void initializePayment_ShouldReturnTrue_WhenDataIsValid() {
        PaymentCreateDto request = new PaymentCreateDto();
        request.setOrderId(100L);
        Payment payment = new Payment();

        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);

        boolean result = paymentService.initializePayment(request);

        assertThat(result).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Process Payment: Should throw exception when payment is already SUCCESSFUL")
    void processPayment_ShouldThrowException_WhenAlreadySuccess() {
        PaymentCreateDto request = new PaymentCreateDto();
        request.setOrderId(100L);
        request.setUserId(1L);
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.SUCCESS);

        doNothing().when(securityUtil).checkOwnership(1L);
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("already SUCCESSFUL");
    }

    @Test
    @DisplayName("Process Payment: Should mark SUCCESS and publish event when random number is even")
    void processPayment_ShouldReturnSuccess_WhenRandomNumberIsEven() {
        PaymentCreateDto request = new PaymentCreateDto();
        request.setOrderId(100L);
        request.setUserId(1L);

        Payment payment = new Payment();
        payment.setId("pay-1");
        payment.setStatus(PaymentStatus.PENDING);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setStatus(PaymentStatus.SUCCESS);

        doNothing().when(securityUtil).checkOwnership(1L);
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));
        when(randomApiClient.fetchRandomNumberString()).thenReturn("4");
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toEvent(payment)).thenReturn(new PaymentStatusEvent());
        when(paymentMapper.toResponseDto(payment)).thenReturn(responseDto);

        PaymentResponseDto result = paymentService.processPayment(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentEventProducer).sendPaymentEvent(any(PaymentStatusEvent.class));
    }

    @Test
    @DisplayName("Process Payment: Should mark FAILED when random number is odd")
    void processPayment_ShouldReturnFailed_WhenRandomNumberIsOdd() {
        PaymentCreateDto request = new PaymentCreateDto();
        request.setOrderId(100L);
        request.setUserId(1L);

        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);

        doNothing().when(securityUtil).checkOwnership(1L);
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(randomApiClient.fetchRandomNumberString()).thenReturn("3");
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setStatus(PaymentStatus.FAILED);
        when(paymentMapper.toResponseDto(payment)).thenReturn(responseDto);

        PaymentResponseDto result = paymentService.processPayment(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("Process Payment: Should fallback to FAILED when API throws Exception")
    void processPayment_ShouldReturnFailed_WhenRandomApiThrowsException() {
        PaymentCreateDto request = new PaymentCreateDto();
        request.setOrderId(100L);

        Payment payment = new Payment();

        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));
        when(randomApiClient.fetchRandomNumberString()).thenThrow(new RuntimeException("API Down"));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toResponseDto(payment)).thenReturn(new PaymentResponseDto());

        paymentService.processPayment(request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Search: Should return single item page when filtering by Order ID")
    void searchPayments_ShouldReturnSingleItem_WhenFilteredByOrderId() {
        PaymentFilterDto filter = new PaymentFilterDto();
        filter.setOrderId(100L);
        Payment payment = new Payment();
        payment.setUserId(1L);

        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(payment));
        doNothing().when(securityUtil).checkOwnership(1L);
        when(paymentMapper.toResponseDto(payment)).thenReturn(new PaymentResponseDto());

        Page<PaymentResponseDto> result = paymentService.searchPayments(filter, Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Search: Should filter by User ID")
    void searchPayments_ShouldFilterByUserId() {
        PaymentFilterDto filter = new PaymentFilterDto();
        filter.setUserId(2L);
        Page<Payment> page = new PageImpl<>(List.of(new Payment()));

        doNothing().when(securityUtil).checkOwnership(2L);
        when(paymentRepository.findAllByUserId(eq(2L), any())).thenReturn(page);
        when(paymentMapper.toResponseDto(any())).thenReturn(new PaymentResponseDto());

        Page<PaymentResponseDto> result = paymentService.searchPayments(filter, Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Search: Should throw AccessDenied when filtering by status and not admin")
    void searchPayments_ShouldThrowException_WhenFilteredByStatusAndNotAdmin() {
        PaymentFilterDto filter = new PaymentFilterDto();
        filter.setStatus(PaymentStatus.SUCCESS);
        Pageable pageable = Pageable.unpaged();

        when(securityUtil.isAdmin()).thenReturn(false);

        assertThatThrownBy(() -> paymentService.searchPayments(filter, pageable))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only administrators can filter globally by payment status");
    }

    @Test
    @DisplayName("Search: Should filter by status when admin")
    void searchPayments_ShouldFilterByStatus_WhenAdmin() {
        PaymentFilterDto filter = new PaymentFilterDto();
        filter.setStatus(PaymentStatus.SUCCESS);
        Page<Payment> page = new PageImpl<>(List.of(new Payment()));

        when(securityUtil.isAdmin()).thenReturn(true);
        when(paymentRepository.findAllByStatus(eq(PaymentStatus.SUCCESS), any())).thenReturn(page);

        paymentService.searchPayments(filter, Pageable.unpaged());
        verify(paymentRepository).findAllByStatus(eq(PaymentStatus.SUCCESS), any());
    }

    @Test
    @DisplayName("Search: Should return all when empty filter and admin")
    void searchPayments_ShouldReturnAll_WhenEmptyFilterAndAdmin() {
        PaymentFilterDto filter = new PaymentFilterDto();
        Page<Payment> page = new PageImpl<>(List.of(new Payment()));

        when(securityUtil.isAdmin()).thenReturn(true);
        when(paymentRepository.findAll(any(Pageable.class))).thenReturn(page);

        paymentService.searchPayments(filter, Pageable.unpaged());
        verify(paymentRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Search: Should return current user payments when empty filter and not admin")
    void searchPayments_ShouldReturnCurrentUserPayments_WhenEmptyFilterAndNotAdmin() {
        PaymentFilterDto filter = new PaymentFilterDto();
        Page<Payment> page = new PageImpl<>(List.of(new Payment()));

        when(securityUtil.isAdmin()).thenReturn(false);
        when(securityUtil.getAuthenticatedUserId()).thenReturn(5L);
        when(paymentRepository.findAllByUserId(eq(5L), any(Pageable.class))).thenReturn(page);

        paymentService.searchPayments(filter, Pageable.unpaged());
        verify(paymentRepository).findAllByUserId(eq(5L), any(Pageable.class));
    }

    @Test
    @DisplayName("Get Total Sum for User: Should return calculated sum")
    void getTotalSumForUser_ShouldReturnSum() {
        PaymentSumView mockView = mock(PaymentSumView.class);
        when(mockView.total()).thenReturn(BigDecimal.valueOf(150.50));

        doNothing().when(securityUtil).checkOwnership(1L);
        when(paymentRepository.calculateTotalSumForUser(eq(1L), any(), any()))
                .thenReturn(Optional.of(mockView));

        BigDecimal result = paymentService.getTotalSumForUser(1L, LocalDateTime.now(), LocalDateTime.now());

        assertThat(result).isEqualTo(BigDecimal.valueOf(150.50));
    }

    @Test
    @DisplayName("Get Total Sum for User: Should return zero when no data")
    void getTotalSumForUser_ShouldReturnZero_WhenEmpty() {
        doNothing().when(securityUtil).checkOwnership(1L);
        when(paymentRepository.calculateTotalSumForUser(any(), any(), any())).thenReturn(Optional.empty());

        BigDecimal result = paymentService.getTotalSumForUser(1L, LocalDateTime.now(), LocalDateTime.now());

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Get Total Sum for All Users: Should return sum")
    void getTotalSumForAllUsers_ShouldReturnSum() {
        PaymentSumView mockView = mock(PaymentSumView.class);
        when(mockView.total()).thenReturn(BigDecimal.valueOf(1000));

        when(paymentRepository.calculateTotalSumForAllUsers(any(), any()))
                .thenReturn(Optional.of(mockView));

        BigDecimal result = paymentService.getTotalSumForAllUsers(LocalDateTime.now(), LocalDateTime.now());

        assertThat(result).isEqualTo(BigDecimal.valueOf(1000));
    }
}