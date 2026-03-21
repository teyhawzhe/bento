package com.lovius.bento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateMenuRequest(
        @NotNull(message = "supplierId 不可為空白")
        Long supplierId,
        @NotBlank(message = "便當名稱不可為空白")
        String name,
        @NotBlank(message = "便當類型不可為空白")
        String category,
        String description,
        @NotNull(message = "價格不可為空白")
        @DecimalMin(value = "0.0", inclusive = false, message = "價格需大於 0")
        BigDecimal price,
        @NotNull(message = "validFrom 不可為空白")
        LocalDate validFrom,
        @NotNull(message = "validTo 不可為空白")
        LocalDate validTo) {}
