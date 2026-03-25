package com.lovius.bento.exception;

import com.lovius.bento.dto.ApiFailedResponse;
import com.lovius.bento.dto.ApiImportFailedResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CsvImportException.class)
    public ResponseEntity<ApiImportFailedResponse> handleCsvImportException(CsvImportException exception) {
        logger.warn(
                "CSV import exception: status={}, line={}, reason={}, message={}",
                exception.getStatus(),
                exception.getFailedAtLine(),
                exception.getReason(),
                exception.getMessage());
        return ResponseEntity.status(exception.getStatus())
                .body(ApiImportFailedResponse.failed(
                        exception.getMessage(),
                        exception.getFailedAtLine(),
                        exception.getReason()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiFailedResponse> handleApiException(ApiException exception) {
        logger.warn("API exception: status={}, message={}", exception.getStatus(), exception.getMessage());
        return ResponseEntity.status(exception.getStatus())
                .body(ApiFailedResponse.failed(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiFailedResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        logger.warn("Validation failed: {}", message, exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiFailedResponse.failed(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiFailedResponse> handleConstraintViolationException(
            ConstraintViolationException exception) {
        logger.warn("Constraint violation: {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiFailedResponse.failed(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiFailedResponse> handleException(Exception exception) {
        logger.error("Unhandled server exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiFailedResponse.failed("系統發生未預期錯誤"));
    }
}
