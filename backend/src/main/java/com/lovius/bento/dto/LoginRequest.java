package com.lovius.bento.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "帳號不可為空白") String username,
        @NotBlank(message = "密碼不可為空白") String password) {
}
