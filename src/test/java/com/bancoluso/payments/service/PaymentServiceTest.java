package com.bancoluso.payments.service;

import com.bancoluso.payments.dto.PaymentEventDto;
import com.bancoluso.payments.dto.PaymentEventResponseDto;
import com.bancoluso.payments.dto.PaymentStatusResponse;
import com.bancoluso.payments.entity.Payment;
import com.bancoluso.payments.entity.PaymentStatus;
import com.bancoluso.payments.exception.PaymentNotFoundException;
import com.bancoluso.payments.mapper.PaymentMapper;
import com.bancoluso.payments.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static com.bancoluso.payments.common.TestConstants.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper mapper;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentEventDto baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = buildRequest(REFERENCE_ID, PaymentStatus.PENDING, EVENT_TIME);
    }

    @Test
    void ingestEvent_newPayment_shouldPersist() {
        when(paymentRepository.findByReferenceId(REFERENCE_ID)).thenReturn(Optional.empty());

        paymentService.ingestEvent(baseRequest);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        Payment saved = captor.getValue();
        assertThat(saved.getReferenceId()).isEqualTo(REFERENCE_ID);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getEventTimestamp()).isEqualTo(EVENT_TIME);
    }

    @Test
    void ingestEvent_trueDuplicate_shouldBeIgnored() {
        Payment existing = existingPayment(PaymentStatus.PENDING, EVENT_TIME);
        when(paymentRepository.findByReferenceId(REFERENCE_ID)).thenReturn(Optional.of(existing));

        paymentService.ingestEvent(baseRequest);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void ingestEvent_newerEventTime_shouldUpdateStatus() {
        Payment existing = existingPayment(PaymentStatus.PENDING, EVENT_TIME);
        when(paymentRepository.findByReferenceId(REFERENCE_ID)).thenReturn(Optional.of(existing));

        baseRequest.setStatus(PaymentStatus.PROCESSING);
        baseRequest.setEventTimestamp(NEWER_EVENT_TIME);

        paymentService.ingestEvent(baseRequest);

        verify(paymentRepository).save(existing);
        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(existing.getEventTimestamp()).isEqualTo(NEWER_EVENT_TIME);
    }

    @Test
    void ingestEvent_staleEvent_shouldBeIgnored() {
        Payment existing = existingPayment(PaymentStatus.PROCESSING, EVENT_TIME);
        when(paymentRepository.findByReferenceId(REFERENCE_ID)).thenReturn(Optional.of(existing));

        baseRequest.setStatus(PaymentStatus.PENDING);
        baseRequest.setEventTimestamp(STALE_EVENT_TIME);

        paymentService.ingestEvent(baseRequest);

        verify(paymentRepository, never()).save(any());

        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    void ingestEvent_equalTimestampDifferentStatus_shouldBeIgnored() {
        Payment existing = existingPayment(PaymentStatus.PENDING, EVENT_TIME);
        when(paymentRepository.findByReferenceId(REFERENCE_ID)).thenReturn(Optional.of(existing));

        baseRequest.setStatus(PaymentStatus.PROCESSING);

        paymentService.ingestEvent(baseRequest);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getPaymentStatus_knownId_returnsStatus() {
        Payment existing = existingPayment(PaymentStatus.SETTLED, EVENT_TIME);
        when(paymentRepository.findByReferenceId(REFERENCE_ID)).thenReturn(Optional.of(existing));

        PaymentStatusResponse response = paymentService.getPaymentStatus(REFERENCE_ID);

        assertThat(response.referenceId()).isEqualTo(REFERENCE_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.SETTLED);
    }

    @Test
    void getPaymentStatus_unknownId_throwsNotFound() {
        when(paymentRepository.findByReferenceId(REFERENCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus(REFERENCE_ID))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining(REFERENCE_ID);
    }

    @Test
    void getPayments_shouldReturnPaginatedPayments() {
        Pageable pageable = PageRequest.of(0, 2);
        Payment payment = existingPayment(PaymentStatus.PENDING, EVENT_TIME);

        Page<Payment> paymentPage =
                new PageImpl<>(
                        List.of(payment),
                        pageable,
                        2
                );

        PaymentEventResponseDto dto = new PaymentEventResponseDto();
        dto.setReferenceId(REFERENCE_ID);
        dto.setAmount(new BigDecimal("1500.00"));
        dto.setCurrency("EUR");
        dto.setDebtorName("João Silva");
        dto.setDebtorIban("PT50000201231234567890154");
        dto.setCreditorIban("PT50000201239876543210154");
        dto.setValueDate(LocalDate.of(2026, 4, 14));
        dto.setStatus(PaymentStatus.PENDING);
        dto.setEventTimestamp(EVENT_TIME);

        when(paymentRepository.findAllByOrderByIdAsc(pageable)).thenReturn(paymentPage);
        when(mapper.toDto(payment)).thenReturn(dto);

        Page<PaymentEventResponseDto> result = paymentService.getPayments(pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent()).containsExactly(dto);
        verify(paymentRepository).findAllByOrderByIdAsc(pageable);
        verify(mapper).toDto(payment);
    }

    private Payment existingPayment(PaymentStatus status, Instant timestamp) {
        return Payment.create(
                REFERENCE_ID,
                new BigDecimal("1500.00"),
                "EUR",
                "João Silva",
                "PT50000201231234567890154",
                "PT50000201239876543210154",
                LocalDate.of(2026, 4, 14),
                status,
                timestamp
        );
    }
}
