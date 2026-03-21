package com.lovius.bento.dto;

import java.time.Instant;

public record SupplierResponse(
        Long id,
        String name,
        String email,
        String phone,
        String contactPerson,
        String businessRegistrationNo,
        boolean isActive,
        Instant createdAt) {}
