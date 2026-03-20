package com.lovius.bento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateEmployeeRequest(
        @NotBlank(message = "帳號不可為空白") String username,
        @NotBlank(message = "姓名不可為空白") String name,
        @NotBlank(message = "Email 不可為空白")
        @Email(message = "Email 格式不正確")
        String email) {
}
