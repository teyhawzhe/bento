package com.lovius.bento.dto;

public record LoginResponse(
        String token,
        String role,
        Long employeeId,
        String username,
        String name) {
}
