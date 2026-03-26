package com.projeto.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        logger.warn("Erro de validação nos campos da requisição");

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        return ResponseEntity.badRequest().body(Map.of(
                "error", "Erro de validação",
                "code", 400,
                "fields", fieldErrors
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Erro de negócio: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(Map.of(
                "error", ex.getMessage(),
                "code", 400
        ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        logger.warn("Tentativa de login com credenciais inválidas");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "E-mail ou senha inválidos.",
                "code", 401
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("Acesso negado: usuário sem permissão para o recurso solicitado");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "Acesso negado.",
                "code", 403
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        logger.error("Erro não esperado durante requisição", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Erro interno no servidor.",
                "code", 500
        ));
    }
}
