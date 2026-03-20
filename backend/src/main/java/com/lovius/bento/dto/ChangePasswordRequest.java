package com.lovius.bento.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "舊密碼不可為空白") String oldPassword,
        @NotBlank(message = "新密碼不可為空白") String newPassword) {
}
