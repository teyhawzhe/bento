package com.lovius.bento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email 不可為空白")
        @Email(message = "Email 格式不正確")
        String email) {
}
