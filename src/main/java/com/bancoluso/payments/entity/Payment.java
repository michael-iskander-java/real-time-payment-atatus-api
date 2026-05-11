package com.bancoluso.payments.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", nullable = false, unique = true, length = 64)
    private String referenceId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, columnDefinition = "bpchar", length = 3)
    private String currency;

    @Column(name = "debtor_name", nullable = false)
    private String debtorName;

    @Column(name = "debtor_iban", nullable = false, length = 34)
    private String debtorIban;

    @Column(name = "creditor_iban", nullable = false, length = 34)
    private String creditorIban;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;


    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Setter
    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Setter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public static Payment create(
            String referenceId,
            BigDecimal amount,
            String currency,
            String debtorName,
            String debtorIban,
            String creditorIban,
            LocalDate valueDate,
            PaymentStatus status,
            Instant eventTimestamp) {

        Payment p = new Payment();
        p.referenceId = referenceId;
        p.amount = amount;
        p.currency = currency;
        p.debtorName = debtorName;
        p.debtorIban = debtorIban;
        p.creditorIban = creditorIban;
        p.valueDate = valueDate;
        p.status = status;
        p.eventTimestamp = eventTimestamp;
        return p;
    }

}
