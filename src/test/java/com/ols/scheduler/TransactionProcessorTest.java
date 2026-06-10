package com.ols.scheduler;

import com.ols.entity.Transaction;
import com.ols.enums.TransactionStatus;
import com.ols.enums.TransactionType;
import com.ols.repository.TransactionRepository;
import com.ols.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionProcessor processor;

    // ===================== processPending — salida anticipada =====================

    // Verifica que cuando no existen transacciones en ningún estado pendiente el método
    // sale inmediatamente sin llamar a findByStatus ni a process
    @Test
    void processPending_shouldDoNothing_whenNoTransactionsPending() {
        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(false);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(false);

        processor.processPending();

        verify(transactionRepository, never()).findByStatus(any());
        verify(transactionService, never()).process(any());
    }

    // Verifica que siempre se consultan los dos estados (RECEIVED y RETRY_PENDING) antes de decidir
    @Test
    void processPending_shouldCheckBothStatuses_beforeReturning() {
        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(false);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(false);

        processor.processPending();

        verify(transactionRepository, times(1)).existsByStatus(TransactionStatus.RECEIVED);
        verify(transactionRepository, times(1)).existsByStatus(TransactionStatus.RETRY_PENDING);
    }

    // ===================== processPending — solo RECEIVED =====================

    // Verifica que cuando solo existen transacciones RECEIVED se consultan y procesan todas
    @Test
    void processPending_shouldProcessReceivedTransactions_whenOnlyReceivedExist() {
        Transaction t1 = buildTransaction(1L, TransactionStatus.RECEIVED);
        Transaction t2 = buildTransaction(2L, TransactionStatus.RECEIVED);

        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(true);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(false);
        when(transactionRepository.findByStatus(TransactionStatus.RECEIVED)).thenReturn(List.of(t1, t2));

        processor.processPending();

        verify(transactionRepository).findByStatus(TransactionStatus.RECEIVED);
        verify(transactionService).process(t1);
        verify(transactionService).process(t2);
    }

    // Verifica que cuando solo hay RECEIVED no se consulta ni procesa RETRY_PENDING
    @Test
    void processPending_shouldNotQueryRetry_whenOnlyReceivedExist() {
        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(true);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(false);
        when(transactionRepository.findByStatus(TransactionStatus.RECEIVED))
                .thenReturn(List.of(buildTransaction(1L, TransactionStatus.RECEIVED)));

        processor.processPending();

        verify(transactionRepository, never()).findByStatus(TransactionStatus.RETRY_PENDING);
    }

    // Verifica que el número de llamadas a process coincide exactamente con el número de transacciones RECEIVED
    @Test
    void processPending_shouldCallProcessOncePerReceivedTransaction() {
        List<Transaction> transactions = List.of(
                buildTransaction(1L, TransactionStatus.RECEIVED),
                buildTransaction(2L, TransactionStatus.RECEIVED),
                buildTransaction(3L, TransactionStatus.RECEIVED)
        );

        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(true);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(false);
        when(transactionRepository.findByStatus(TransactionStatus.RECEIVED)).thenReturn(transactions);

        processor.processPending();

        verify(transactionService, times(3)).process(any(Transaction.class));
    }

    // ===================== processPending — solo RETRY_PENDING =====================

    // Verifica que cuando solo existen transacciones RETRY_PENDING se consultan y procesan todas
    @Test
    void processPending_shouldProcessRetryTransactions_whenOnlyRetryPendingExist() {
        Transaction t1 = buildTransaction(1L, TransactionStatus.RETRY_PENDING);
        Transaction t2 = buildTransaction(2L, TransactionStatus.RETRY_PENDING);

        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(false);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(true);
        when(transactionRepository.findByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(List.of(t1, t2));

        processor.processPending();

        verify(transactionRepository).findByStatus(TransactionStatus.RETRY_PENDING);
        verify(transactionService).process(t1);
        verify(transactionService).process(t2);
    }

    // Verifica que cuando solo hay RETRY_PENDING no se consulta ni procesa RECEIVED
    @Test
    void processPending_shouldNotQueryReceived_whenOnlyRetryPendingExist() {
        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(false);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(true);
        when(transactionRepository.findByStatus(TransactionStatus.RETRY_PENDING))
                .thenReturn(List.of(buildTransaction(1L, TransactionStatus.RETRY_PENDING)));

        processor.processPending();

        verify(transactionRepository, never()).findByStatus(TransactionStatus.RECEIVED);
    }

    // Verifica que el número de llamadas a process coincide exactamente con el número de transacciones RETRY_PENDING
    @Test
    void processPending_shouldCallProcessOncePerRetryTransaction() {
        List<Transaction> retries = List.of(
                buildTransaction(1L, TransactionStatus.RETRY_PENDING),
                buildTransaction(2L, TransactionStatus.RETRY_PENDING)
        );

        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(false);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(true);
        when(transactionRepository.findByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(retries);

        processor.processPending();

        verify(transactionService, times(2)).process(any(Transaction.class));
    }

    // ===================== processPending — ambos estados =====================

    // Verifica que cuando existen transacciones en ambos estados se procesan RECEIVED y RETRY_PENDING
    @Test
    void processPending_shouldProcessBothGroups_whenBothStatusesExist() {
        Transaction received = buildTransaction(1L, TransactionStatus.RECEIVED);
        Transaction retry = buildTransaction(2L, TransactionStatus.RETRY_PENDING);

        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(true);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(true);
        when(transactionRepository.findByStatus(TransactionStatus.RECEIVED)).thenReturn(List.of(received));
        when(transactionRepository.findByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(List.of(retry));

        processor.processPending();

        verify(transactionRepository).findByStatus(TransactionStatus.RECEIVED);
        verify(transactionRepository).findByStatus(TransactionStatus.RETRY_PENDING);
        verify(transactionService).process(received);
        verify(transactionService).process(retry);
    }

    // Verifica que el total de llamadas a process es la suma de ambas listas cuando los dos estados están presentes
    @Test
    void processPending_shouldCallProcessForAllTransactions_whenBothGroupsExist() {
        List<Transaction> receivedList = List.of(
                buildTransaction(1L, TransactionStatus.RECEIVED),
                buildTransaction(2L, TransactionStatus.RECEIVED)
        );
        List<Transaction> retryList = List.of(
                buildTransaction(3L, TransactionStatus.RETRY_PENDING)
        );

        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(true);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(true);
        when(transactionRepository.findByStatus(TransactionStatus.RECEIVED)).thenReturn(receivedList);
        when(transactionRepository.findByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(retryList);

        processor.processPending();

        verify(transactionService, times(3)).process(any(Transaction.class));
    }

    // ===================== processPending — listas vacías =====================

    // Verifica que si existsByStatus retorna true pero findByStatus devuelve lista vacía, process no se llama
    @Test
    void processPending_shouldNotCallProcess_whenFindByStatusReturnsEmptyList() {
        when(transactionRepository.existsByStatus(TransactionStatus.RECEIVED)).thenReturn(true);
        when(transactionRepository.existsByStatus(TransactionStatus.RETRY_PENDING)).thenReturn(false);
        when(transactionRepository.findByStatus(TransactionStatus.RECEIVED)).thenReturn(List.of());

        processor.processPending();

        verify(transactionService, never()).process(any());
    }

    // ===================== helpers =====================

    private Transaction buildTransaction(Long id, TransactionStatus status) {
        return Transaction.builder()
                .id(id)
                .externalId("EXT-" + id)
                .transactionType(TransactionType.PAYMENT)
                .status(status)
                .sourceSystem("SYSTEM_A")
                .receivedAt(LocalDateTime.of(2026, 6, 7, 10, 0))
                .payload("{\"amount\": 100}")
                .createdAt(LocalDateTime.of(2026, 6, 7, 10, 0, 1))
                .build();
    }
}
