package com.lovius.bento.dto;

public record ImportSupplierResult(
        Long id,
        String name,
        String email,
        String phone,
        String contactPerson,
        String businessRegistrationNo,
        boolean isActive) {
}
