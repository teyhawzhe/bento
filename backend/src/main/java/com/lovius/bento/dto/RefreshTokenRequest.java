package com.lovius.bento.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken 不可為空白") String refreshToken) {
}
