package com.lovius.bento.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String role,
        Long employeeId,
        String username,
        String name) {
}
