package com.bancoluso.payments.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class PaymentEventResponseDto extends PaymentEventDto {
    private Instant createdAt;
    private Instant updatedAt;
}
