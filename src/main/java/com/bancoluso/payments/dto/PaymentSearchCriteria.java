package com.bancoluso.payments.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentSearchCriteria(
        String referenceId,
        String currency,
        String debtorName,
        String debtorIban,
        String creditorIban,
        String status,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        LocalDate fromValueDate,
        LocalDate toValueDate,
        LocalDate fromEventTimestamp,
        LocalDate toEventTimestamp
) {}