package com.lovius.bento.dto;

import java.time.Instant;
import java.time.LocalDate;

public record OrderResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Long menuId,
        String menuName,
        LocalDate orderDate,
        Long createdBy,
        Instant createdAt) {}
