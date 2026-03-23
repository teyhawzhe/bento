package com.lovius.bento.model;

import java.time.Instant;

public class Employee {
    private Long id;
    private Long departmentId;
    private String departmentName;
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
            Long departmentId,
            String departmentName,
            String username,
            String passwordHash,
            String name,
            String email,
            boolean admin,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
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
