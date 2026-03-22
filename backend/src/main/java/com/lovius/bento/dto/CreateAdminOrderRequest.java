package com.lovius.bento.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateAdminOrderRequest(
        @NotNull(message = "employeeId 不可為空白")
        Long employeeId,
        @NotNull(message = "menuId 不可為空白")
        Long menuId,
        @NotNull(message = "orderDate 不可為空白")
        LocalDate orderDate) {}
