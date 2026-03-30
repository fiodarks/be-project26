package com.github.fiodarks.project26.archive.adapter.in.web;

import com.github.fiodarks.project26.adapter.in.web.dto.ErrorResponse;
import com.github.fiodarks.project26.archive.application.exception.ForbiddenOperationException;
import com.github.fiodarks.project26.archive.application.exception.NotFoundException;
import com.github.fiodarks.project26.archive.application.exception.ValidationException;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.validation.ConstraintViolationException;
import java.util.Locale;

@ControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final Environment env;

    public ApiExceptionHandler(Environment env) {
        this.env = env;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", "Request validation failed"));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenOperationException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler({AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("UNAUTHORIZED", "Authentication required"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", "Request validation failed"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", "Invalid parameter value"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        var maxUploadSize = e.getMaxUploadSize();
        var suffix = maxUploadSize > 0 ? " (max: %s)".formatted(formatBytes(maxUploadSize)) : "";

        long contentLength = -1;
        String contentType = null;
        String requestUri = null;
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            var request = servletAttrs.getRequest();
            contentLength = request.getContentLengthLong();
            contentType = request.getContentType();
            requestUri = request.getRequestURI();
        }

        var configuredMaxFileSize = env.getProperty("spring.servlet.multipart.max-file-size");
        var configuredMaxRequestSize = env.getProperty("spring.servlet.multipart.max-request-size");

        var rootCause = rootCause(e);
        var rootCauseClass = rootCause == null ? null : rootCause.getClass().getName();
        var rootCauseMessage = rootCause == null ? null : rootCause.getMessage();

        log.warn(
                "Upload rejected: payload too large{} uri='{}' contentType='{}' contentLength={} configuredMaxFileSize='{}' configuredMaxRequestSize='{}' maxUploadSizeBytes={} exceptionMessage='{}' rootCauseClass='{}' rootCauseMessage='{}'",
                suffix,
                requestUri,
                contentType,
                contentLength,
                configuredMaxFileSize,
                configuredMaxRequestSize,
                maxUploadSize,
                e.getMessage(),
                rootCauseClass,
                rootCauseMessage
        );
        var response = new ErrorResponse("PAYLOAD_TOO_LARGE", "Uploaded file is too large" + suffix)
                .putDetailsItem("contentLength", Long.toString(contentLength))
                .putDetailsItem("contentType", contentType == null ? "unknown" : contentType)
                .putDetailsItem("uri", requestUri == null ? "unknown" : requestUri);
        if (configuredMaxFileSize != null) {
            response.putDetailsItem("configuredMaxFileSize", configuredMaxFileSize);
        }
        if (configuredMaxRequestSize != null) {
            response.putDetailsItem("configuredMaxRequestSize", configuredMaxRequestSize);
        }
        response.putDetailsItem("maxUploadSizeBytes", Long.toString(maxUploadSize));
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            response.putDetailsItem("exceptionMessage", e.getMessage());
        }
        if (rootCauseClass != null && !rootCauseClass.isBlank()) {
            response.putDetailsItem("rootCauseClass", rootCauseClass);
        }
        if (rootCauseMessage != null && !rootCauseMessage.isBlank()) {
            response.putDetailsItem("rootCauseMessage", rootCauseMessage);
        }

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    private static Throwable rootCause(Throwable t) {
        if (t == null) {
            return null;
        }
        var current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == t ? null : current;
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        var provided = e.getContentType() == null ? "unknown" : e.getContentType().toString();
        var supported = e.getSupportedMediaTypes() == null || e.getSupportedMediaTypes().isEmpty()
                ? "unknown"
                : e.getSupportedMediaTypes().toString();

        var message = "Unsupported Content-Type: '%s'. Supported: %s. For photo uploads use 'multipart/form-data'."
                .formatted(provided, supported);

        log.warn("Request rejected: unsupported media type. Provided='{}' Supported={}", provided, supported);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("UNSUPPORTED_MEDIA_TYPE", message));
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "unknown";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex]);
    }
}
