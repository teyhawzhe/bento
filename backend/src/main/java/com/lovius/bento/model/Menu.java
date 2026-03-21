package com.lovius.bento.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class Menu {
    private Long id;
    private Long supplierId;
    private String name;
    private String category;
    private String description;
    private BigDecimal price;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public Menu(
            Long id,
            Long supplierId,
            String name,
            String category,
            String description,
            BigDecimal price,
            LocalDate validFrom,
            LocalDate validTo,
            Long createdBy,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.supplierId = supplierId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touchUpdatedAt(Instant instant) {
        this.updatedAt = instant;
    }
}
