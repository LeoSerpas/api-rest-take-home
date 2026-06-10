package com.ols.service;

import com.ols.dto.TransactionFilterDTO;
import com.ols.dto.TransactionRequestDTO;
import com.ols.dto.TransactionResponseDTO;
import com.ols.dto.TransactionSummaryDTO;
import com.ols.entity.Transaction;
import com.ols.entity.TransactionEvent;
import com.ols.enums.TransactionStatus;
import com.ols.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.ols.dto.TransactionEventDetailDTO;
import com.ols.dto.TransactionEventDTO;
import com.ols.repository.TransactionEventRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository transactionEventRepository;

    /**
     * Registra una nueva transacción validando idempotencia.
     */
    public TransactionResponseDTO register(TransactionRequestDTO request) {

        if (transactionRepository.existsByExternalId(request.getExternalId())) {
                throw new IllegalStateException(
                "La transacción con externalId '" + request.getExternalId() + "' ya fue registrada."
                );
        }

        Transaction transaction = Transaction.builder()
                .externalId(request.getExternalId())
                .transactionType(request.getTransactionType())
                .status(TransactionStatus.RECEIVED)
                .sourceSystem(request.getSourceSystem())
                .receivedAt(request.getReceivedAt())
                .payload(request.getPayload())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        saveEvent(saved, TransactionStatus.RECEIVED, "Transaccion recibida");
        log.info("ID: {} | Transaccion registrada con estado RECEIVED", saved.getId());

        return TransactionResponseDTO.builder()
                .id(saved.getId())
                .externalId(saved.getExternalId())
                .transactionType(saved.getTransactionType())
                .status(saved.getStatus())
                .sourceSystem(saved.getSourceSystem())
                .receivedAt(saved.getReceivedAt())
                .createdAt(saved.getCreatedAt())
                .build();
        }

    /**
     * Lista transacciones con filtros opcionales y paginación.
     */
    public Page<TransactionSummaryDTO> findAll(TransactionFilterDTO filters) {

        Pageable pageable = PageRequest.of(
                filters.getPage(),
                filters.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt") // Más recientes primero
        );

        return transactionRepository.findWithFilters(
                filters.getStatus(),
                filters.getTransactionType(),
                filters.getSourceSystem(),
                filters.getDateFrom(),
                filters.getDateTo(),
                pageable
        ).map(t -> TransactionSummaryDTO.builder()
                .id(t.getId())
                .externalId(t.getExternalId())
                .transactionType(t.getTransactionType())
                .status(t.getStatus())
                .sourceSystem(t.getSourceSystem())
                .receivedAt(t.getReceivedAt())
                .createdAt(t.getCreatedAt())
                .build()
        );
    }

    /**
     * Consulta el detalle de una transacción por ID incluyendo su historial de eventos.
     */
    public TransactionEventDetailDTO findById(Long id) {

        // Buscar la transacción o lanzar error si no existe
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                    "No se encontró la transacción con id: " + id
                ));

        // Obtener historial de eventos
        List<TransactionEventDTO> events = transactionEventRepository
                .findByTransactionIdOrderByCreatedAtAsc(id)
                .stream()
                .map(e -> TransactionEventDTO.builder()
                        .id(e.getId())
                        .status(e.getStatus())
                        .description(e.getDescription())
                        .createdAt(e.getCreatedAt())
                        .build()
                )
                .toList();

        // Mapear Entity -> DetailDTO
        return TransactionEventDetailDTO.builder()
                .id(transaction.getId())
                .externalId(transaction.getExternalId())
                .transactionType(transaction.getTransactionType())
                .sourceSystem(transaction.getSourceSystem())
                .receivedAt(transaction.getReceivedAt())
                .createdAt(transaction.getCreatedAt())
                .payload(transaction.getPayload())
                .status(transaction.getStatus())
                .errorMessage(transaction.getErrorMessage())
                .events(events)
                .build();
    }

    /**
    * Procesa una transacción cambiando sus estados y registrando eventos.
    * Simula éxito o fallo de forma aleatoria.
    */
    public void process(Transaction transaction) {

        log.info(">>> Procesando transacción ID: {} | Estado actual: {}",
            transaction.getId(), transaction.getStatus());
        

        // Cambiar a PROCESSING
        transaction.setStatus(TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);
        saveEvent(transaction, TransactionStatus.PROCESSING, "Iniciando procesamiento");
        log.info(">>> ID: {} | Cambiando a PROCESSING", transaction.getId());

        // Simular procesamiento (70% éxito, 30% fallo)
        boolean success = Math.random() > 0.5;

        if (success) {
                transaction.setStatus(TransactionStatus.PROCESSED);
                transactionRepository.save(transaction);
                saveEvent(transaction, TransactionStatus.PROCESSED, "Transacción procesada exitosamente");
                log.info(">>> ID: {} | Cambiando a PROCESSED", transaction.getId());
        } else {
                // Simular si va a FAILED o RETRY_PENDING (50/50)
                boolean retry = Math.random() > 0.5;

                if (retry) {
                transaction.setStatus(TransactionStatus.RETRY_PENDING);
                transactionRepository.save(transaction);
                saveEvent(transaction, TransactionStatus.RETRY_PENDING, "Fallo temporal, reintento pendiente");
                log.info(">>> ID: {} | Cambiando a RETRY_PENDING", transaction.getId());
                } else {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setErrorMessage("Error simulado durante el procesamiento");
                transactionRepository.save(transaction);
                saveEvent(transaction, TransactionStatus.FAILED, "Transacción fallida");
                log.info(">>> ID: {} | Cambiando a FAILED", transaction.getId());
                }
        }
    }

    /**
    * Guarda un evento en el historial de la transacción.
    */
    private void saveEvent(Transaction transaction, TransactionStatus status, String description) {
        TransactionEvent event = TransactionEvent.builder()
                .transaction(transaction)
                .status(status)
                .description(description)
                .build();
        transactionEventRepository.save(event);
    }

    /**
    * Reprocesa una transacción fallida manualmente.
    * Solo permite reprocesar transacciones en estado FAILED.
    */
    public TransactionEventDetailDTO reprocess(Long id) {

        // Buscar la transacción o lanzar error si no existe
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la transacción con id: " + id
                ));

        // Validar que solo se reprocesen transacciones en estado FAILED
        if (transaction.getStatus() != TransactionStatus.FAILED) {
                throw new IllegalStateException(
                "La transacción con id: " + id + " no puede reprocesarse. " +
                "Estado actual: " + transaction.getStatus() + ". " +
                "Solo se pueden reprocesar transacciones en estado FAILED."
                );
        }

        // Registrar el intento de reproceso
        saveEvent(transaction, TransactionStatus.FAILED, "Reproceso solicitado manualmente");
        log.info(">>> ID: {} | Reproceso solicitado manualmente", transaction.getId());

        // Llamar al proceso existente
        process(transaction);

        // Retornar el detalle actualizado
        return findById(id);
    }
 
}