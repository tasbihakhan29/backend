package com.medsync.config;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.postgresql.util.PSQLException;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PSQLException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseError(PSQLException ex) {
        String message = "Database connection error";
        
        if (ex.getMessage().contains("FATAL")) {
            message = "Database authentication failed. Check your credentials.";
        } else if (ex.getMessage().contains("timeout")) {
            message = "Database connection timeout. Server may be unreachable.";
        } else if (ex.getMessage().contains("Connection refused")) {
            message = "Database connection refused. Server is not accessible.";
        } else if (ex.getMessage().contains("SSL")) {
            message = "SSL/TLS connection error. Check your network.";
        }
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "message", message,
                "error", "DATABASE_ERROR"
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Database connection")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "success", false,
                    "message", ex.getMessage(),
                    "error", "DATABASE_UNAVAILABLE"
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Validation failed",
                "errors", errors
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
        ));
    }
}
