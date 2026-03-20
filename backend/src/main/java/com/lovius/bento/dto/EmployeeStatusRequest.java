package com.lovius.bento.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

public record EmployeeStatusRequest(
        @JsonAlias({"is_active", "isActive"})
        @NotNull(message = "狀態不可為空白")
        Boolean isActive) {
}
