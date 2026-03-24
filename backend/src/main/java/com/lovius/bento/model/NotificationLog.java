package com.lovius.bento.model;

import java.time.Instant;
import java.time.LocalDate;

public class NotificationLog {
    private Long id;
    private final LocalDate notifyDate;
    private final String emailTo;
    private final String content;
    private final String status;
    private final String errorMessage;
    private final Instant createdAt;

    public NotificationLog(
            Long id,
            LocalDate notifyDate,
            String emailTo,
            String content,
            String status,
            String errorMessage,
            Instant createdAt) {
        this.id = id;
        this.notifyDate = notifyDate;
        this.emailTo = emailTo;
        this.content = content;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getNotifyDate() {
        return notifyDate;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public String getContent() {
        return content;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
