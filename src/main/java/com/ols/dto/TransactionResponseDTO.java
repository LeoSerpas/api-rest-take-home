package com.ols.dto;

import com.ols.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;
import com.ols.enums.TransactionStatus;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonPropertyOrder({"id", "externalId", "transactionType", "sourceSystem", "receivedAt", "createdAt", "status"})
public class TransactionResponseDTO {

    private Long id;
    private String externalId;
    private TransactionType transactionType;
    private String sourceSystem;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
    private TransactionStatus status;
}