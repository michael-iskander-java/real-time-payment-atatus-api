package com.bancoluso.payments.mapper;

import com.bancoluso.payments.dto.PaymentEventResponseDto;
import com.bancoluso.payments.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentEventResponseDto toDto(Payment entity);

}
