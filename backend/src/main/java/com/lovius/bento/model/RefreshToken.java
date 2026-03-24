package com.lovius.bento.model;

import java.time.Instant;

public class RefreshToken {
    private Long id;
    private final Long employeeId;
    private final String tokenHash;
    private final Instant expiresAt;
    private boolean revoked;
    private final Instant createdAt;

    public RefreshToken(
            Long id,
            Long employeeId,
            String tokenHash,
            Instant expiresAt,
            boolean revoked,
            Instant createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
