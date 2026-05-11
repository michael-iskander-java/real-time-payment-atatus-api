package com.bancoluso.payments.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponseDto(int status, String error, String message, List<String> details, Instant timestamp) {}
