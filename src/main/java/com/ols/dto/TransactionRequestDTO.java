package com.ols.dto;

import com.ols.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDTO {

    @NotBlank(message = "El identificador externo es obligatorio")
    private String externalId;

    @NotNull(message = "El tipo de transacción es obligatorio")
    private TransactionType transactionType;

    @NotBlank(message = "El sistema origen es obligatorio")
    private String sourceSystem;

    @NotNull(message = "La fecha/hora de recepción es obligatoria")
    private LocalDateTime receivedAt;

    @NotBlank(message = "El payload no puede estar vacío")
    private String payload;
}