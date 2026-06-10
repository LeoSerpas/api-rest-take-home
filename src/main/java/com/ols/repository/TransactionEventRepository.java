package com.ols.repository;

import com.ols.entity.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, Long> {

    /**
     * Obtiene todos los eventos de una transacción ordenados por fecha ascendente.
     * Así el historial muestra primero el evento más antiguo.
     */
    List<TransactionEvent> findByTransactionIdOrderByCreatedAtAsc(Long transactionId);
}
