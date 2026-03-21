package com.lovius.bento.model;

import java.time.Instant;
import java.time.LocalDate;

public record MonthlyBillingLogView(
        Long id,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        Long supplierId,
        String supplierName,
        String emailTo,
        String status,
        String errorMessage,
        Long triggeredBy,
        Instant sentAt,
        Instant createdAt) {}
