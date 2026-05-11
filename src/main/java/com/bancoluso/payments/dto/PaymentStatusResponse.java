package com.bancoluso.payments.dto;

import com.bancoluso.payments.entity.PaymentStatus;

import java.time.Instant;

public record PaymentStatusResponse(String referenceId, PaymentStatus status, Instant eventTimestamp, Instant updatedAt) {}
