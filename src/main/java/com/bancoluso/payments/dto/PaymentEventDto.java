package com.bancoluso.payments.dto;

import com.bancoluso.payments.entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class PaymentEventDto {

        @NotBlank(message = "referenceId is required")
        @Size(max = 64, message = "referenceId must be at most 64 characters")
        private String referenceId;

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        @Digits(integer = 15, fraction = 4, message = "amount has too many digits")
        private BigDecimal amount;

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
        private String currency;

        @NotBlank(message = "debtorName is required")
        private String debtorName;

        @NotBlank(message = "debtorIban is required")
        @Size(max = 34, message = "debtorIban too long")
        private String debtorIban;

        @NotBlank(message = "creditorIban is required")
        @Size(max = 34, message = "creditorIban too long")
        private String creditorIban;

        @NotNull(message = "valueDate is required")
        private LocalDate valueDate;

        @NotNull(message = "status is required")
        private PaymentStatus status;

        @NotNull(message = "eventTimestamp is required")
        private Instant eventTimestamp;
}
