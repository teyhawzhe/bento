package com.lovius.bento.dto;

import java.time.Instant;

public record EmployeeSummaryResponse(
        Long id,
        String username,
        String name,
        String email,
        boolean isAdmin,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt) {
}
