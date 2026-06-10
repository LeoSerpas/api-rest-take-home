package com.ols.enums;

public enum TransactionStatus {
    RECEIVED,       // Transacción recibida por el sistema
    PROCESSING,     // En proceso de ser procesada
    PROCESSED,      // Procesada exitosamente
    FAILED,         // Falló el procesamiento
    RETRY_PENDING   // Falló pero está pendiente de reintento
}
