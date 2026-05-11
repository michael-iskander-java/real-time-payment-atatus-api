package com.bancoluso.payments.common;

import com.bancoluso.payments.dto.PaymentEventDto;
import com.bancoluso.payments.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class TestConstants {

    public static final String EVENT_URL = "/v1/payments/events";
    public static final String STATUS_URL = "/v1/payments/{referenceId}/status";
    public static final String REFERENCE_ID = "TXN-2026-INT-001";
    public static final Instant EVENT_TIME = Instant.parse("2026-04-14T09:00:00Z");
    public static final Instant NEWER_EVENT_TIME = Instant.parse("2026-04-14T10:00:00Z");
    public static final Instant STALE_EVENT_TIME = Instant.parse("2026-04-14T08:00:00Z");
    public static final int CONSUMER_TIMEOUT_SECONDS = 15;


    public static PaymentEventDto buildRequest(String referenceId, PaymentStatus status, Instant timestamp) {
        PaymentEventDto req = new PaymentEventDto();
        req.setReferenceId(referenceId);
        req.setAmount(new BigDecimal("1500.00"));
        req.setCurrency("EUR");
        req.setDebtorName("João Silva");
        req.setDebtorIban("PT50000201231234567890154");
        req.setCreditorIban("PT50000201239876543210154");
        req.setValueDate(LocalDate.of(2026, 4, 14));
        req.setStatus(status);
        req.setEventTimestamp(timestamp);
        return req;
    }
}
