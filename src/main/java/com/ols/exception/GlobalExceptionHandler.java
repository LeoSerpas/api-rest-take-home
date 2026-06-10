package com.ols.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String CAMPO = "El campo '";

    // Maneja transacciones duplicadas
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            TIMESTAMP, LocalDateTime.now(),
            STATUS, 409,
            ERROR, "Conflict",
            MESSAGE, ex.getMessage()
        ));
    }

    // Maneja cualquier otro error inesperado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            TIMESTAMP, LocalDateTime.now(),
            STATUS, 500,
            ERROR, "Internal Server Error",
            MESSAGE, "Ocurrió un error inesperado"
        ));
    }
   @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = "Valor inválido en el cuerpo de la petición.";

        String causeMessage = ex.getCause() != null ? ex.getCause().getMessage() : "";

        // Enum inválido
        if (causeMessage.contains("not one of the values accepted for Enum class")) {

            String field = "desconocido";
            java.util.regex.Matcher fieldMatcher = java.util.regex.Pattern
                    .compile("\\[\"(\\w+)\"\\]")
                    .matcher(causeMessage);
            while (fieldMatcher.find()) {
                field = fieldMatcher.group(1);
            }

            String validValues = "";
            java.util.regex.Matcher valuesMatcher = java.util.regex.Pattern
                    .compile("Enum class: \\[([A-Z, ]+)\\]")
                    .matcher(causeMessage);
            if (valuesMatcher.find()) {
                validValues = valuesMatcher.group(1);
            }

            message = CAMPO + field + "' tiene un valor inválido. "
                    + "Los valores permitidos son: " + validValues;

        // Fecha/hora inválida
        } else if (causeMessage.contains("LocalDateTime") || causeMessage.contains("LocalDate")
                || causeMessage.contains("Cannot deserialize value of type `java.time")) {

            String field = "desconocido";
            java.util.regex.Matcher fieldMatcher = java.util.regex.Pattern
                    .compile("\\[\"(\\w+)\"\\]")
                    .matcher(causeMessage);
            while (fieldMatcher.find()) {
                field = fieldMatcher.group(1);
            }

            message = CAMPO + field + "' tiene un formato de fecha inválido. "
                    + "El formato esperado es: yyyy-MM-ddTHH:mm:ss (Ejemplo: 2026-06-07T17:00:00)";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            TIMESTAMP, LocalDateTime.now(),
            STATUS, 400,
            ERROR, "Bad Request",
            MESSAGE, message
        ));
    }
    // Maneja parámetros de URL con tipo inválido (enums, números, etc. Para el api con detalle Paginado)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> {
                    // Si tiene mensaje de validación (@NotBlank, @NotNull, etc.)
                    if (e.getRejectedValue() == null || e.getDefaultMessage() == null) {
                        return CAMPO + e.getField() + "' tiene un valor inválido.";
                    }
                    // Si es error de conversión de tipo (enum, número, etc.)
                    if (e.getDefaultMessage().contains("Failed to convert")) {
                        return "El parámetro '" + e.getField() + "' tiene un valor inválido: '"
                                + e.getRejectedValue() + "'.";
                    }
                    // Mensaje normal de validación
                    return e.getField() + ": " + e.getDefaultMessage();
                })
                .collect(java.util.stream.Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            TIMESTAMP, LocalDateTime.now(),
            STATUS, 400,
            ERROR, "Bad Request",
            MESSAGE, errors
        ));
    }
    
    // En TransactionService — cuando no existe el ID solicitado, se lanza IllegalArgumentException con mensaje "Transacción no encontrada con ID: {id}"    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            TIMESTAMP, LocalDateTime.now(),
            STATUS, 404,
            ERROR, "Not Found",
            MESSAGE, ex.getMessage()
        ));
    }

}