package com.lovius.bento.model;

import java.time.Instant;

public class Employee {
    private Long id;
    private String username;
    private String passwordHash;
    private String name;
    private String email;
    private boolean admin;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public Employee(
            Long id,
            String username,
            String passwordHash,
            String name,
            String email,
            boolean admin,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.name = name;
        this.email = email;
        this.admin = admin;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touchUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
