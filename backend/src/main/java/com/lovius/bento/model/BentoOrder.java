package com.lovius.bento.model;

import java.time.Instant;
import java.time.LocalDate;

public class BentoOrder {
    private Long id;
    private Long employeeId;
    private Long menuId;
    private LocalDate orderDate;
    private Long createdBy;
    private Instant createdAt;

    public BentoOrder(
            Long id,
            Long employeeId,
            Long menuId,
            LocalDate orderDate,
            Long createdBy,
            Instant createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.menuId = menuId;
        this.orderDate = orderDate;
        this.createdBy = createdBy;
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

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
