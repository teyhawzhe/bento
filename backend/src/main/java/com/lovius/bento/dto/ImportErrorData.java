package com.lovius.bento.dto;

public record ImportErrorData(
        String message,
        Integer failedAtLine,
        String reason) {
}
