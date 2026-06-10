package com.ols.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ===================== handleDuplicate =====================

    // Verifica que una IllegalStateException retorna HTTP 409 Conflict con el mensaje de la excepción
    @Test
    void handleDuplicate_shouldReturn409WithConflictBody() {
        IllegalStateException ex = new IllegalStateException("Transacción duplicada: EXT-001");

        ResponseEntity<Map<String, Object>> response = handler.handleDuplicate(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("status", 409);
        assertThat(response.getBody()).containsEntry("error", "Conflict");
        assertThat(response.getBody()).containsEntry("message", "Transacción duplicada: EXT-001");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // Verifica que el mensaje propagado en handleDuplicate es exactamente el de la excepción
    @Test
    void handleDuplicate_shouldPropagateExceptionMessage() {
        IllegalStateException ex = new IllegalStateException("Ya existe una transacción con externalId EXT-999");

        ResponseEntity<Map<String, Object>> response = handler.handleDuplicate(ex);

        assertThat(response.getBody().get("message"))
                .isEqualTo("Ya existe una transacción con externalId EXT-999");
    }

    // ===================== handleGeneric =====================

    // Verifica que cualquier Exception genérica retorna HTTP 500 con el mensaje fijo sin exponer detalles internos
    @Test
    void handleGeneric_shouldReturn500WithFixedMessage() {
        Exception ex = new RuntimeException("Detalle sensible interno que no debe exponerse");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("error", "Internal Server Error");
        assertThat(response.getBody()).containsEntry("message", "Ocurrió un error inesperado");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // Verifica que el mensaje real de la excepción nunca se expone al cliente en el error genérico
    @Test
    void handleGeneric_shouldNeverExposeInternalExceptionMessage() {
        Exception ex = new NullPointerException("null ref en línea 42");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertThat((String) response.getBody().get("message"))
                .isEqualTo("Ocurrió un error inesperado")
                .doesNotContain("null ref");
    }

    // ===================== handleNotReadable =====================

    // Verifica que cuando la causa es null se retorna el mensaje genérico de cuerpo inválido
    @Test
    void handleNotReadable_shouldReturnGenericMessage_whenCauseIsNull() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Not readable", (Throwable) null, null);

        ResponseEntity<Map<String, Object>> response = handler.handleNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "Bad Request");
        assertThat(response.getBody().get("message"))
                .isEqualTo("Valor inválido en el cuerpo de la petición.");
    }

    // Verifica que cuando el valor de un enum es inválido se informa el nombre del campo y los valores permitidos
    @Test
    void handleNotReadable_shouldReturnEnumError_whenInvalidEnumValue() {
        String causeMsg = "Cannot deserialize value; not one of the values accepted for Enum class: " +
                "[PAYMENT, REFUND, ADJUSTMENT] (through reference chain: Request[\"transactionType\"])";
        Throwable cause = new RuntimeException(causeMsg);
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Not readable", cause, null);

        ResponseEntity<Map<String, Object>> response = handler.handleNotReadable(ex);

        String message = (String) response.getBody().get("message");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(message).contains("transactionType");
        assertThat(message).contains("PAYMENT, REFUND, ADJUSTMENT");
    }

    // Verifica que cuando el formato de LocalDateTime es inválido se informa el campo y el formato esperado
    @Test
    void handleNotReadable_shouldReturnDateFormatError_whenInvalidLocalDateTime() {
        String causeMsg = "Cannot deserialize value of type `java.time.LocalDateTime` from String \"bad-date\"" +
                " (through reference chain: Request[\"receivedAt\"])";
        Throwable cause = new RuntimeException(causeMsg);
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Not readable", cause, null);

        ResponseEntity<Map<String, Object>> response = handler.handleNotReadable(ex);

        String message = (String) response.getBody().get("message");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(message).contains("receivedAt");
        assertThat(message).contains("yyyy-MM-ddTHH:mm:ss");
    }

    // Verifica que cuando el mensaje de causa no coincide con ningún patrón se retorna el mensaje genérico
    @Test
    void handleNotReadable_shouldReturnGenericMessage_whenCauseMessageIsUnrecognized() {
        Throwable cause = new RuntimeException("Otro tipo de error de deserialización desconocido");
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Not readable", cause, null);

        ResponseEntity<Map<String, Object>> response = handler.handleNotReadable(ex);

        assertThat(response.getBody().get("message"))
                .isEqualTo("Valor inválido en el cuerpo de la petición.");
    }

    // ===================== handleValidation =====================

    // Verifica que un FieldError con rejectedValue nulo retorna el mensaje de campo inválido genérico
    @Test
    void handleValidation_shouldReturnFieldInvalid_whenRejectedValueIsNull() {
        FieldError fieldError = new FieldError("obj", "amount", null, false, null, null, "must not be null");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "Bad Request");
        assertThat(response.getBody().get("message")).isEqualTo("El campo 'amount' tiene un valor inválido.");
    }

    // Verifica que un FieldError con defaultMessage nulo retorna el mensaje de campo inválido genérico
    @Test
    void handleValidation_shouldReturnFieldInvalid_whenDefaultMessageIsNull() {
        FieldError fieldError = new FieldError("obj", "status", "INVALID_VALUE", false, null, null, null);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getBody().get("message")).isEqualTo("El campo 'status' tiene un valor inválido.");
    }

    // Verifica que un error de conversión de tipo retorna el nombre del parámetro y el valor rechazado
    @Test
    void handleValidation_shouldReturnParamInvalid_whenFailedToConvert() {
        FieldError fieldError = new FieldError("obj", "type", "UNKNOWN",
                false, null, null, "Failed to convert value of type 'java.lang.String' to required type");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getBody().get("message"))
                .isEqualTo("El parámetro 'type' tiene un valor inválido: 'UNKNOWN'.");
    }

    // Verifica que un error de validación normal retorna "campo: mensaje" con el mensaje de la anotación
    @Test
    void handleValidation_shouldReturnNormalValidationMessage() {
        FieldError fieldError = new FieldError("obj", "externalId", "someValue",
                false, null, null, "must not be blank");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getBody().get("message")).isEqualTo("externalId: must not be blank");
    }

    // Verifica que múltiples errores de validación se concatenan separados por ", "
    @Test
    void handleValidation_shouldJoinMultipleErrorsWithComma() {
        FieldError error1 = new FieldError("obj", "externalId", "val1",
                false, null, null, "must not be blank");
        FieldError error2 = new FieldError("obj", "sourceSystem", "val2",
                false, null, null, "must not be null");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        String message = (String) response.getBody().get("message");
        assertThat(message).contains("externalId: must not be blank");
        assertThat(message).contains("sourceSystem: must not be null");
        assertThat(message).contains(", ");
    }

    // ===================== handleNotFound =====================

    // Verifica que una IllegalArgumentException retorna HTTP 404 Not Found con el mensaje de la excepción
    @Test
    void handleNotFound_shouldReturn404WithNotFoundBody() {
        IllegalArgumentException ex = new IllegalArgumentException("Transacción no encontrada con ID: 99");

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("error", "Not Found");
        assertThat(response.getBody()).containsEntry("message", "Transacción no encontrada con ID: 99");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    // Verifica que el mensaje propagado en handleNotFound es exactamente el de la excepción
    @Test
    void handleNotFound_shouldPropagateExceptionMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Transacción no encontrada con ID: 42");

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertThat(response.getBody().get("message"))
                .isEqualTo("Transacción no encontrada con ID: 42");
    }
}
