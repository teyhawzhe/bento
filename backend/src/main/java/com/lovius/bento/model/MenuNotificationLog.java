package com.lovius.bento.model;

import java.time.Instant;
import java.time.LocalDate;

public class MenuNotificationLog {
    private Long id;
    private final LocalDate notifyDate;
    private final LocalDate missingFrom;
    private final LocalDate missingTo;
    private final String status;
    private final Instant createdAt;

    public MenuNotificationLog(
            Long id,
            LocalDate notifyDate,
            LocalDate missingFrom,
            LocalDate missingTo,
            String status,
            Instant createdAt) {
        this.id = id;
        this.notifyDate = notifyDate;
        this.missingFrom = missingFrom;
        this.missingTo = missingTo;
        this.status = status;
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

    public LocalDate getMissingFrom() {
        return missingFrom;
    }

    public LocalDate getMissingTo() {
        return missingTo;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
