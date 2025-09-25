package com.location.server.api;

import com.location.server.service.AssignmentConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ApiErrorHandler {
  private ResponseEntity<Map<String, Object>> toResponse(HttpStatus status, String message, String path) {
    return ResponseEntity.status(status)
        .body(
            Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", path));
  }

  @ExceptionHandler(AssignmentConflictException.class)
  public ResponseEntity<Map<String, Object>> handleConflict(
      AssignmentConflictException ex, HttpServletRequest request) {
    return toResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    return toResponse(HttpStatus.BAD_REQUEST, "Validation error", request.getRequestURI());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    return toResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
  }
}
