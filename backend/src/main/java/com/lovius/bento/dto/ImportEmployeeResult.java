package com.lovius.bento.dto;

public record ImportEmployeeResult(
        Long id,
        String username,
        String name,
        String email,
        Long departmentId,
        boolean isAdmin,
        boolean isActive) {
}
