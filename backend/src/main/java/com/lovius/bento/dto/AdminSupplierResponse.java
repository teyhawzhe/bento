package com.lovius.bento.dto;

import java.time.Instant;
import java.util.List;

public record AdminSupplierResponse(
        Long id,
        String name,
        String email,
        String phone,
        String contactPerson,
        String businessRegistrationNo,
        boolean isActive,
        Instant createdAt,
        List<AdminSupplierMenuOptionResponse> menuOptions) {}
