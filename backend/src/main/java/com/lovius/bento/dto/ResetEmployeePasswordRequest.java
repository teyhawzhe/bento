package com.lovius.bento.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetEmployeePasswordRequest(
        @NotBlank(message = "新密碼不可為空白") String newPassword) {
}
