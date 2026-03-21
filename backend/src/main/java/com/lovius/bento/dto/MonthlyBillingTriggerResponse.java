package com.lovius.bento.dto;

import java.time.LocalDate;

public record MonthlyBillingTriggerResponse(
        String message,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        int supplierCount,
        int recipientCount,
        int failedCount) {}
