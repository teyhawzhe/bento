package com.lovius.bento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportMenuResult(
        Long id,
        Long supplierId,
        String name,
        String category,
        String description,
        BigDecimal price,
        LocalDate validFrom,
        LocalDate validTo) {
}
