package com.lovius.bento.model;

import java.time.Instant;

public class ErrorNotificationEmail {
    private Long id;
    private final String email;
    private final Long createdBy;
    private final Instant createdAt;

    public ErrorNotificationEmail(Long id, String email, Long createdBy, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
