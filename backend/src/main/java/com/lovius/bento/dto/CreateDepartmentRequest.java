package com.lovius.bento.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDepartmentRequest(
        @NotBlank(message = "部門名稱為必填")
        String name) {
}
