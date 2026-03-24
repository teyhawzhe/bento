package com.lovius.bento.dto;

public record ApiSuccessResponse<T>(String status, T data) {
    public static <T> ApiSuccessResponse<T> success(T data) {
        return new ApiSuccessResponse<>("success", data);
    }

    public static ApiSuccessResponse<Void> empty() {
        return new ApiSuccessResponse<>("success", null);
    }
}
