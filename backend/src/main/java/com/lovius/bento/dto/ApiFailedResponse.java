package com.lovius.bento.dto;

public record ApiFailedResponse(String status, ApiFailedData data) {
    public static ApiFailedResponse failed(String message) {
        return new ApiFailedResponse("failed", new ApiFailedData(message));
    }
}
