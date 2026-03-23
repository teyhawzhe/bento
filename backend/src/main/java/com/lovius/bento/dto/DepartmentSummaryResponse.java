package com.lovius.bento.dto;

import java.time.Instant;

public record DepartmentSummaryResponse(
        Long id,
        String name,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt) {
}
