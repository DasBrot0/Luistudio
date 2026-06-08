package com.luistudio.reservas.exception;

import com.luistudio.reservas.dto.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus();
        ApiError body = new ApiError(
            OffsetDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        log.warn(
            "http_error endpoint={} status={} message={}",
            request.getRequestURI(),
            status.value(),
            sanitize(ex.getMessage())
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        ApiError body = new ApiError(
            OffsetDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            request.getRequestURI()
        );
        log.warn(
            "http_validation_error endpoint={} status={} message={}",
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value(),
            sanitize(message)
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDatabase(DataAccessException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
            OffsetDateTime.now(),
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
            "No se pudo acceder a la base de datos. Verifica que el servicio este disponible y que el esquema inicial haya sido aplicado.",
            request.getRequestURI()
        );
        log.error(
            "http_database_error endpoint={} status={} message={}",
            request.getRequestURI(),
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            sanitize(ex.getMessage()),
            ex
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        ApiError body = new ApiError(
            OffsetDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "Ocurrio un error interno. Intenta nuevamente o contacta al administrador.",
            request.getRequestURI()
        );
        log.error(
            "http_unexpected_error endpoint={} status={} message={}",
            request.getRequestURI(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            sanitize(ex.getMessage()),
            ex
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "n/a";
        }
        return message.replaceAll("[\\r\\n]+", " ");
    }
}
