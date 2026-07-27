package com.footballstats.backend.controller;

import com.footballstats.backend.dto.ApiErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getAllErrors().stream()
            .findFirst()
            .map(err -> err.getDefaultMessage())
            .orElse("Некорректные данные.");
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
            java.time.OffsetDateTime.now(), HttpStatus.BAD_REQUEST.value(), message, request.getRequestURI(), fieldErrors
        ));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwt(JwtException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Невалидный или истекший токен.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Недостаточно прав доступа.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Некорректное значение параметра: " + ex.getName() + ".", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        String message = ex.getReason() == null || ex.getReason().isBlank() ? "Не удалось выполнить запрос." : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(
            ApiErrorResponse.of(ex.getStatusCode().value(), message, request.getRequestURI())
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
            .findFirst()
            .map(violation -> violation.getMessage())
            .orElse("Некорректные данные.");
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Некорректный формат запроса.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data constraint rejected {} {}", request.getMethod(), request.getRequestURI());
        return error(HttpStatus.CONFLICT, "Данные конфликтуют с уже существующей записью.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error(
            "Unhandled API error for {} {} type={}",
            request.getMethod(),
            request.getRequestURI(),
            ex.getClass().getSimpleName()
        );
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера.", request);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        String safeMessage = message == null || message.isBlank() ? "Не удалось выполнить запрос." : message;
        return ResponseEntity.status(status).body(ApiErrorResponse.of(status.value(), safeMessage, request.getRequestURI()));
    }
}
