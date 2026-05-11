package com.bancoluso.payments.service;

import com.bancoluso.payments.dto.PaymentEventDto;
import com.bancoluso.payments.dto.PaymentEventResponseDto;
import com.bancoluso.payments.dto.PaymentStatusResponse;
import com.bancoluso.payments.entity.Payment;
import com.bancoluso.payments.exception.PaymentNotFoundException;
import com.bancoluso.payments.mapper.PaymentMapper;
import com.bancoluso.payments.repository.PaymentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.Optional;


@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper mapper;

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Transactional
    public void ingestEvent(PaymentEventDto request) {
        Optional<Payment> existing = paymentRepository.findByReferenceId(request.getReferenceId());

        if (existing.isEmpty()) {
            Payment payment = Payment.create(
                    request.getReferenceId(),
                    request.getAmount(),
                    request.getCurrency(),
                    request.getDebtorName(),
                    request.getDebtorIban(),
                    request.getCreditorIban(),
                    request.getValueDate(),
                    request.getStatus(),
                    request.getEventTimestamp()
            );
            paymentRepository.save(payment);
            log.info("Created new payment [referenceId={}] with status [{}]", request.getReferenceId(), request.getStatus());
            return;
        }

        Payment payment = existing.get();

        if (payment.getStatus() == request.getStatus()) {
            log.info("Ignoring true duplicate event [referenceId={}, status={}]", request.getReferenceId(), request.getStatus());
            return;
        }

        if (!request.getEventTimestamp().isAfter(payment.getEventTimestamp())) {
            log.warn("Ignoring stale/out-of-order event [referenceId={}, incomingStatus={}, " +
                            "incomingTimestamp={}, storedTimestamp={}]",
                    request.getReferenceId(), request.getStatus(),
                    request.getEventTimestamp(), payment.getEventTimestamp());
            return;
        }


        log.info("Updating payment [referenceId={}] from status [{}] to [{}]", request.getReferenceId(), payment.getStatus(), request.getStatus());

        payment.setStatus(request.getStatus());
        payment.setEventTimestamp(request.getEventTimestamp());
        paymentRepository.save(payment);
    }

    public PaymentStatusResponse getPaymentStatus(String referenceId) {
        Payment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new PaymentNotFoundException(referenceId));
        return new PaymentStatusResponse(
                payment.getReferenceId(),
                payment.getStatus(),
                payment.getEventTimestamp(),
                payment.getUpdatedAt());
    }

    public Page<PaymentEventResponseDto> getPayments(Pageable pageable) {

        return paymentRepository.findAllByOrderByIdAsc(pageable).map(mapper::toDto);
    }
}