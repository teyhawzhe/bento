package com.lovius.bento.model;

import java.time.Instant;

public class Department {
    private Long id;
    private String name;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public Department(
            Long id,
            String name,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.name = name;
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

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public void setName(String name) {
        this.name = name;
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
