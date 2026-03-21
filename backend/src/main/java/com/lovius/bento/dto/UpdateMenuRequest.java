package com.lovius.bento.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateMenuRequest(
        Long supplierId,
        String name,
        String category,
        String description,
        @DecimalMin(value = "0.0", inclusive = false, message = "價格需大於 0")
        BigDecimal price,
        LocalDate validFrom,
        LocalDate validTo) {}
