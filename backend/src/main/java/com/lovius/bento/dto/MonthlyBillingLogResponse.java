package com.lovius.bento.dto;

import java.time.Instant;
import java.time.LocalDate;

public record MonthlyBillingLogResponse(
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
