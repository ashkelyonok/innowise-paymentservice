package org.ashkelyonok.paymentservice.mapper;

import org.ashkelyonok.paymentservice.model.event.PaymentStatusEvent;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentCreateDto;
import org.ashkelyonok.paymentservice.model.dto.response.PaymentResponseDto;
import org.ashkelyonok.paymentservice.model.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    Payment toEntity(PaymentCreateDto request);

    PaymentResponseDto toResponseDto(Payment payment);

    @Mapping(source = "id", target = "paymentId")
    @Mapping(source = "status", target = "status")
    PaymentStatusEvent toEvent(Payment payment);
}