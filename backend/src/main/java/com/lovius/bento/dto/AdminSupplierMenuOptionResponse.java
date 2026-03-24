package com.lovius.bento.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AdminSupplierMenuOptionResponse(
        Long id,
        String name,
        String category,
        String description,
        BigDecimal price,
        LocalDate validFrom,
        LocalDate validTo,
        Instant updatedAt) {}
