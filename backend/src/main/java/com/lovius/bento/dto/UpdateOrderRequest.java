package com.lovius.bento.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderRequest(
        @NotNull(message = "menuId 不可為空白")
        Long menuId) {}
