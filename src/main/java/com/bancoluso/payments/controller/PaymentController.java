package com.bancoluso.payments.controller;

import com.bancoluso.payments.dto.PaymentEventDto;
import com.bancoluso.payments.dto.PaymentEventResponseDto;
import com.bancoluso.payments.dto.PaymentSearchCriteria;
import com.bancoluso.payments.dto.PaymentStatusResponse;
import com.bancoluso.payments.messaging.PaymentEventProducer;
import com.bancoluso.payments.service.PaymentService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@RestController
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    @PostMapping("/events")
    public ResponseEntity<Void> ingestEvent(@Valid @RequestBody PaymentEventDto request) {
        paymentEventProducer.produce(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{referenceId}/status")
    public ResponseEntity<PaymentStatusResponse> getStatus(@PathVariable String referenceId) {
        PaymentStatusResponse response = paymentService.getPaymentStatus(referenceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"","/"})
    public ResponseEntity<Page<PaymentEventResponseDto>> getPayments(PaymentSearchCriteria criteria,
                                                                     Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPayments(criteria, pageable));
    }

}
