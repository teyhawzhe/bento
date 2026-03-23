package com.lovius.bento.dto;

public record UpdateEmployeeResponse(
        String message,
        EmployeeSummaryResponse employee) {
}
