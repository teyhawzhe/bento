package com.lovius.bento.model;

import java.time.Instant;
import java.time.LocalDate;

public class MonthlyBillingLog {
    private Long id;
    private final LocalDate billingPeriodStart;
    private final LocalDate billingPeriodEnd;
    private final Long supplierId;
    private final String emailTo;
    private final String status;
    private final String errorMessage;
    private final Long triggeredBy;
    private final Instant sentAt;
    private final Instant createdAt;

    public MonthlyBillingLog(
            Long id,
            LocalDate billingPeriodStart,
            LocalDate billingPeriodEnd,
            Long supplierId,
            String emailTo,
            String status,
            String errorMessage,
            Long triggeredBy,
            Instant sentAt,
            Instant createdAt) {
        this.id = id;
        this.billingPeriodStart = billingPeriodStart;
        this.billingPeriodEnd = billingPeriodEnd;
        this.supplierId = supplierId;
        this.emailTo = emailTo;
        this.status = status;
        this.errorMessage = errorMessage;
        this.triggeredBy = triggeredBy;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getBillingPeriodStart() {
        return billingPeriodStart;
    }

    public LocalDate getBillingPeriodEnd() {
        return billingPeriodEnd;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getTriggeredBy() {
        return triggeredBy;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
