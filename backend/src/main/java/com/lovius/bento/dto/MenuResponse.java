package com.lovius.bento.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MenuResponse(
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
        Instant updatedAt) {}
