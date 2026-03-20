package com.lovius.bento.dto;

public record EmployeeCreatedResponse(
        String message,
        EmployeeSummaryResponse employee,
        String generatedPassword) {
}
