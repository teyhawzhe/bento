package com.lovius.bento.dto;

public record ApiImportFailedResponse(
        String status,
        ImportErrorData data) {
    public static ApiImportFailedResponse failed(String message, Integer failedAtLine, String reason) {
        return new ApiImportFailedResponse(
                "failed",
                new ImportErrorData(message, failedAtLine, reason));
    }
}
