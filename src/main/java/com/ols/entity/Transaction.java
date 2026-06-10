package com.ols.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import com.ols.enums.TransactionType;
import com.ols.enums.TransactionStatus;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador externo único (idempotencia).
     */
    @NotBlank(message = "El identificador externo es obligatorio")
    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    /**
     * Tipo de transacción válido (validado por enum).
     */
    @NotNull(message = "El tipo de transacción es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    /**
     * Sistema origen que envía la transacción.
     */
    @NotBlank(message = "El sistema origen es obligatorio")
    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    /**
     * Fecha/hora de recepción de la transacción.
     */
    @NotNull(message = "La fecha/hora de recepción es obligatoria")
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    /**
     * Payload o detalle de la transacción (no puede estar vacío).
     */
    @NotBlank(message = "El payload no puede estar vacío")
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Fecha/hora de registro en BD (se asigna automáticamente).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Estado actual de la transacción.
     */
    @NotNull(message = "El estado de la transacción es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Mensaje de error si la transacción falló.
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;
}

