package com.ols.dto;

import com.ols.enums.TransactionStatus;
import com.ols.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonPropertyOrder({"id", "externalId", "transactionType", "status", "sourceSystem", "receivedAt", "createdAt"})
public class TransactionSummaryDTO {

    private Long id;
    private String externalId;
    private TransactionType transactionType;
    private TransactionStatus status;
    private String sourceSystem;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;

    // Nota: no incluye payload porque puede ser muy pesado en una lista
}