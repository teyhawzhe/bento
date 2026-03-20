package com.lovius.bento.dto;

import java.util.List;

public record ImportEmployeesResponse(
        String message,
        int successCount,
        int failureCount,
        List<ImportedEmployeeError> errors) {
}
