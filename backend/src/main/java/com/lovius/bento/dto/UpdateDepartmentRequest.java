package com.lovius.bento.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateDepartmentRequest(
        @NotBlank(message = "部門名稱為必填")
        String name,
        @JsonAlias({"is_active", "isActive"})
        @NotNull(message = "部門狀態為必填")
        Boolean isActive) {
}
