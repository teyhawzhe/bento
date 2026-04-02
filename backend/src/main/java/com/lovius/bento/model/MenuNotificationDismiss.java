package com.lovius.bento.model;

import java.time.Instant;
import java.time.LocalDate;

public class MenuNotificationDismiss {
    private final LocalDate dismissDate;
    private final Instant createdAt;

    public MenuNotificationDismiss(LocalDate dismissDate, Instant createdAt) {
        this.dismissDate = dismissDate;
        this.createdAt = createdAt;
    }

    public LocalDate getDismissDate() {
        return dismissDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
