package com.lovius.bento.dto;

public record ImportedEmployeeError(int lineNumber, String rawData, String reason) {
}
