package com.lovius.bento.model;

import java.time.Instant;

public class Supplier {
    private Long id;
    private final String name;
    private final String email;
    private final String phone;
    private final String contactPerson;
    private final String businessRegistrationNo;
    private final boolean active;
    private final Instant createdAt;

    public Supplier(
            Long id,
            String name,
            String email,
            String phone,
            String contactPerson,
            String businessRegistrationNo,
            boolean active,
            Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.contactPerson = contactPerson;
        this.businessRegistrationNo = businessRegistrationNo;
        this.active = active;
        this.createdAt = createdAt;
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

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getBusinessRegistrationNo() {
        return businessRegistrationNo;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
