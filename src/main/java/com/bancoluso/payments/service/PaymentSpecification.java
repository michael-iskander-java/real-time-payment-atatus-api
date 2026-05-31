package com.bancoluso.payments.service;

import com.bancoluso.payments.dto.PaymentSearchCriteria;
import com.bancoluso.payments.entity.Payment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.bancoluso.payments.entity.Payment_.*;

public class PaymentSpecification {

    public static Specification<Payment> filter(PaymentSearchCriteria paymentSearchCriteria) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (paymentSearchCriteria.referenceId() != null) {
                predicates.add(cb.equal(root.get(REFERENCE_ID), paymentSearchCriteria.referenceId()));
            }

            if (paymentSearchCriteria.status() != null) {
                predicates.add(cb.equal(root.get(STATUS), paymentSearchCriteria.status()));
            }

            if (paymentSearchCriteria.currency() != null) {
                predicates.add(cb.equal(root.get(CURRENCY), paymentSearchCriteria.currency()));
            }

            if (paymentSearchCriteria.debtorName() != null) {
                String safe = paymentSearchCriteria.debtorName().toLowerCase()
                        .replace("\\", "\\\\")
                        .replace("%",  "\\%")
                        .replace("_",  "\\_");
                predicates.add(cb.like(cb.lower(root.get("debtorName")), "%" + safe + "%", '\\'));
            }

            if (paymentSearchCriteria.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(AMOUNT), paymentSearchCriteria.minAmount()));
            }

            if (paymentSearchCriteria.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(AMOUNT), paymentSearchCriteria.maxAmount()));
            }

            if (paymentSearchCriteria.fromValueDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(VALUE_DATE), paymentSearchCriteria.fromValueDate()));
            }

            if (paymentSearchCriteria.toValueDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(VALUE_DATE), paymentSearchCriteria.toValueDate()));
            }

            if (paymentSearchCriteria.fromEventTimestamp() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(EVENT_TIMESTAMP).as(LocalDate.class), paymentSearchCriteria.fromEventTimestamp()));
            }

            if (paymentSearchCriteria.toEventTimestamp() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(EVENT_TIMESTAMP).as(LocalDate.class), paymentSearchCriteria.toEventTimestamp()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}