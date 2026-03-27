package org.ashkelyonok.paymentservice.mapper;

import org.ashkelyonok.paymentservice.model.event.PaymentStatusEvent;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentCreateDto;
import org.ashkelyonok.paymentservice.model.dto.response.PaymentResponseDto;
import org.ashkelyonok.paymentservice.model.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper interface for converting between Payment entities and DTOs.
 * Provides mapping methods for request DTOs, response DTOs, and Kafka events.
 * Unmapped target properties are ignored to avoid compilation warnings.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    /**
     * Converts a PaymentCreateDto to a Payment entity.
     *
     * @param request the payment creation request containing order ID, user ID, and payment amount
     * @return a Payment entity with fields mapped from the request DTO
     */
    Payment toEntity(PaymentCreateDto request);

    /**
     * Converts a Payment entity to a PaymentResponseDto.
     *
     * @param payment the payment entity to convert
     * @return a PaymentResponseDto containing payment details (ID, order ID, user ID, status, timestamp, amount)
     */
    PaymentResponseDto toResponseDto(Payment payment);

    /**
     * Converts a Payment entity to a PaymentStatusEvent for Kafka messaging.
     *
     * @param payment the payment entity to convert
     * @return a PaymentStatusEvent containing payment ID, order ID, user ID, status, timestamp, and amount
     */
    @Mapping(source = "id", target = "paymentId")
    @Mapping(source = "status", target = "status")
    PaymentStatusEvent toEvent(Payment payment);
}