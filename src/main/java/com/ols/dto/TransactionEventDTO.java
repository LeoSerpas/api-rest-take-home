package com.ols.dto;

import com.ols.enums.TransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Getter
@Builder
@JsonPropertyOrder({"id", "status", "description", "createdAt"})
public class TransactionEventDTO {

    private Long id;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
}
