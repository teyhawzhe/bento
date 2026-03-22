package com.lovius.bento.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AdminOrderResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Long menuId,
        String menuName,
        Long supplierId,
        String supplierName,
        BigDecimal menuPrice,
        LocalDate orderDate,
        Long createdBy,
        String createdByName,
        Instant createdAt) {}
