package com.ols.scheduler;

import com.ols.entity.Transaction;
import com.ols.enums.TransactionStatus;
import com.ols.repository.TransactionRepository;
import com.ols.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessor {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    /**
     * Revisa cada 10 segundos si hay transacciones pendientes.
     * Solo procesa si realmente hay trabajo, evitando uso innecesario de recursos.
     */
    @Scheduled(fixedDelay = 10000)
    public void processPending() {

        // Generar traceId nuevo al inicio de cada ciclo, antes de cualquier verificación
        MDC.put("traceId", UUID.randomUUID().toString().substring(0, 8));

        try {
            boolean hasReceived = transactionRepository.existsByStatus(TransactionStatus.RECEIVED);
            boolean hasRetry = transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING);

            if (!hasReceived && !hasRetry) {
                return;
            }

            if (hasReceived) {
                List<Transaction> pending = transactionRepository.findByStatus(TransactionStatus.RECEIVED);
                log.info("Procesando {} transacciones en estado RECEIVED.", pending.size());
                pending.forEach(transactionService::process);
            }

            if (hasRetry) {
                List<Transaction> retries = transactionRepository.findByStatus(TransactionStatus.RETRY_PENDING);
                log.info("Reintentando {} transacciones en estado RETRY_PENDING.", retries.size());
                retries.forEach(transactionService::process);
            }

        } finally {
            MDC.clear();
        }
    }
}