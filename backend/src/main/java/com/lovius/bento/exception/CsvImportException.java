package com.lovius.bento.exception;

import org.springframework.http.HttpStatus;

public class CsvImportException extends RuntimeException {
    private final HttpStatus status;
    private final Integer failedAtLine;
    private final String reason;

    public CsvImportException(HttpStatus status, String message, Integer failedAtLine, String reason) {
        super(message);
        this.status = status;
        this.failedAtLine = failedAtLine;
        this.reason = reason;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Integer getFailedAtLine() {
        return failedAtLine;
    }

    public String getReason() {
        return reason;
    }
}
