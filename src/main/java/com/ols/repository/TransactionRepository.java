package com.ols.repository;

import com.ols.entity.Transaction;
import com.ols.enums.TransactionStatus;
import com.ols.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Busca por identificador externo (idempotencia).
     */
    Optional<Transaction> findByExternalId(String externalId);

    /**
     * Verifica si ya existe una transacción con ese identificador externo.
     */
    boolean existsByExternalId(String externalId);

    /**
     * Lista transacciones con filtros opcionales y paginación.
     */
    @Query("SELECT t FROM Transaction t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:transactionType IS NULL OR t.transactionType = :transactionType) AND " +
           "(:sourceSystem IS NULL OR t.sourceSystem = :sourceSystem) AND " +
           "(:dateFrom IS NULL OR t.receivedAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR t.receivedAt <= :dateTo)")
    Page<Transaction> findWithFilters(
            @Param("status") TransactionStatus status,
            @Param("transactionType") TransactionType transactionType,
            @Param("sourceSystem") String sourceSystem,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable
    );

    /**
     * Busca todas las transacciones por estado.
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Verifica si existe alguna transacción con ese estado.
     * Más eficiente que findByStatus cuando solo necesitas saber si hay algo pendiente.
     */
    boolean existsByStatus(TransactionStatus status);

}