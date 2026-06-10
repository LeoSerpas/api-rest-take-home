package com.ols.dto;

import com.ols.enums.TransactionStatus;
import com.ols.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Getter
@Builder
@JsonPropertyOrder({"id", "externalId", "transactionType", "sourceSystem", "receivedAt", "createdAt", "payload", "status", "errorMessage", "events"})
public class TransactionEventDetailDTO {

    // Datos generales
    private Long id;
    private String externalId;
    private TransactionType transactionType;
    private String sourceSystem;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;

    // Payload completo
    private String payload;

    // Estado actual
    private TransactionStatus status;

    // Mensaje de error si aplica
    private String errorMessage;

    // Historial de eventos
    private List<TransactionEventDTO> events;
}