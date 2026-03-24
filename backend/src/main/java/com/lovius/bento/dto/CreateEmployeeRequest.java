package com.lovius.bento.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEmployeeRequest(
        @NotBlank(message = "帳號為必填")
        String username,
        @NotBlank(message = "姓名為必填")
        String name,
        @NotBlank(message = "Email 為必填")
        @Email(message = "Email 格式不正確")
        String email,
        @JsonAlias({"department_id", "departmentId"})
        @NotNull(message = "部門為必填")
        Long departmentId) {
}
