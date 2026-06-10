package com.ols.service;

import com.ols.dto.*;
import com.ols.entity.Transaction;
import com.ols.entity.TransactionEvent;
import com.ols.enums.TransactionStatus;
import com.ols.enums.TransactionType;
import com.ols.repository.TransactionEventRepository;
import com.ols.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionEventRepository transactionEventRepository;

    @InjectMocks
    private TransactionService transactionService;

    private TransactionRequestDTO request;
    private Transaction savedTransaction;

    @BeforeEach
    void setUp() {
        request = new TransactionRequestDTO(
                "EXT-001",
                TransactionType.PAYMENT,
                "SYSTEM_A",
                LocalDateTime.of(2026, 6, 7, 10, 0),
                "{\"amount\": 100}"
        );

        savedTransaction = Transaction.builder()
                .id(1L)
                .externalId("EXT-001")
                .transactionType(TransactionType.PAYMENT)
                .status(TransactionStatus.RECEIVED)
                .sourceSystem("SYSTEM_A")
                .receivedAt(LocalDateTime.of(2026, 6, 7, 10, 0))
                .payload("{\"amount\": 100}")
                .createdAt(LocalDateTime.of(2026, 6, 7, 10, 0, 1))
                .build();
    }

    // ===================== register =====================

    @Test
    void register_shouldReturnResponseDTO_whenExternalIdIsNew() {
        when(transactionRepository.existsByExternalId("EXT-001")).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponseDTO response = transactionService.register(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getExternalId()).isEqualTo("EXT-001");
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.RECEIVED);
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.PAYMENT);
        assertThat(response.getSourceSystem()).isEqualTo("SYSTEM_A");
        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionEventRepository).save(any(TransactionEvent.class));
    }

    @Test
    void register_shouldThrowIllegalStateException_whenExternalIdAlreadyExists() {
        when(transactionRepository.existsByExternalId("EXT-001")).thenReturn(true);

        assertThatThrownBy(() -> transactionService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXT-001");

        verify(transactionRepository, never()).save(any());
        verify(transactionEventRepository, never()).save(any());
    }

    @Test
    void register_shouldSaveEventWithStatusReceived() {
        when(transactionRepository.existsByExternalId("EXT-001")).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        transactionService.register(request);

        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(transactionEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(TransactionStatus.RECEIVED);
    }

    // ===================== findAll =====================

    @Test
    void findAll_shouldReturnPageOfSummaryDTOs() {
        TransactionFilterDTO filters = new TransactionFilterDTO();
        filters.setPage(0);
        filters.setSize(5);

        Page<Transaction> page = new PageImpl<>(List.of(savedTransaction));
        when(transactionRepository.findWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<TransactionSummaryDTO> result = transactionService.findAll(filters);

        assertThat(result.getContent()).hasSize(1);
        TransactionSummaryDTO dto = result.getContent().get(0);
        assertThat(dto.getExternalId()).isEqualTo("EXT-001");
        assertThat(dto.getStatus()).isEqualTo(TransactionStatus.RECEIVED);
        assertThat(dto.getTransactionType()).isEqualTo(TransactionType.PAYMENT);
    }

    @Test
    void findAll_shouldPassFiltersToRepository() {
        TransactionFilterDTO filters = new TransactionFilterDTO();
        filters.setStatus(TransactionStatus.FAILED);
        filters.setTransactionType(TransactionType.REFUND);
        filters.setSourceSystem("SYSTEM_B");
        filters.setPage(1);
        filters.setSize(20);

        when(transactionRepository.findWithFilters(
                eq(TransactionStatus.FAILED),
                eq(TransactionType.REFUND),
                eq("SYSTEM_B"),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        Page<TransactionSummaryDTO> result = transactionService.findAll(filters);

        assertThat(result).isEmpty();
    }

    // ===================== findById =====================

    @Test
    void findById_shouldReturnDetailWithEvents_whenTransactionExists() {
        TransactionEvent event = TransactionEvent.builder()
                .id(10L)
                .transaction(savedTransaction)
                .status(TransactionStatus.RECEIVED)
                .description("Transaccion recibida")
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(savedTransaction));
        when(transactionEventRepository.findByTransactionIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(event));

        TransactionEventDetailDTO detail = transactionService.findById(1L);

        assertThat(detail.getId()).isEqualTo(1L);
        assertThat(detail.getExternalId()).isEqualTo("EXT-001");
        assertThat(detail.getPayload()).isEqualTo("{\"amount\": 100}");
        assertThat(detail.getStatus()).isEqualTo(TransactionStatus.RECEIVED);
        assertThat(detail.getEvents()).hasSize(1);
        assertThat(detail.getEvents().get(0).getStatus()).isEqualTo(TransactionStatus.RECEIVED);
        assertThat(detail.getEvents().get(0).getDescription()).isEqualTo("Transaccion recibida");
    }

    @Test
    void findById_shouldReturnEmptyEventList_whenNoEventsExist() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(savedTransaction));
        when(transactionEventRepository.findByTransactionIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of());

        TransactionEventDetailDTO detail = transactionService.findById(1L);

        assertThat(detail.getEvents()).isEmpty();
    }

    @Test
    void findById_shouldThrowIllegalArgumentException_whenNotFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    // ===================== process =====================

    @RepeatedTest(10)
    void process_shouldAlwaysTransitionThroughProcessing() {
        Transaction transaction = buildTransaction(TransactionStatus.RECEIVED);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        transactionService.process(transaction);

        // El primer evento guardado siempre debe ser PROCESSING
        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(transactionEventRepository, atLeast(2)).save(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues().get(0).getStatus()).isEqualTo(TransactionStatus.PROCESSING);
    }

    @RepeatedTest(10)
    void process_shouldAlwaysEndInAValidFinalStatus() {
        Transaction transaction = buildTransaction(TransactionStatus.RECEIVED);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        transactionService.process(transaction);

        assertThat(transaction.getStatus()).isIn(
                TransactionStatus.PROCESSED,
                TransactionStatus.FAILED,
                TransactionStatus.RETRY_PENDING
        );
    }

    @RepeatedTest(10)
    void process_shouldSetErrorMessage_whenTransactionFails() {
        Transaction transaction = buildTransaction(TransactionStatus.RECEIVED);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        transactionService.process(transaction);

        if (transaction.getStatus() == TransactionStatus.FAILED) {
            assertThat(transaction.getErrorMessage()).isNotBlank();
        }
    }

    @RepeatedTest(10)
    void process_shouldSaveAtLeastTwice_processingPlusFinalState() {
        Transaction transaction = buildTransaction(TransactionStatus.RECEIVED);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        transactionService.process(transaction);

        // PROCESSING + estado final (siempre 2 saves)
        verify(transactionRepository, times(2)).save(transaction);
    }

    // ===================== reprocess =====================

    @Test
    void reprocess_shouldThrowIllegalArgumentException_whenTransactionNotFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.reprocess(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void reprocess_shouldThrowIllegalStateException_whenStatusIsNotFailed() {
        Transaction transaction = buildTransaction(TransactionStatus.PROCESSED);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.reprocess(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    void reprocess_shouldThrowIllegalStateException_whenStatusIsReceived() {
        Transaction transaction = buildTransaction(TransactionStatus.RECEIVED);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.reprocess(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    void reprocess_shouldThrowIllegalStateException_whenStatusIsRetryPending() {
        Transaction transaction = buildTransaction(TransactionStatus.RETRY_PENDING);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.reprocess(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAILED");
    }

    @RepeatedTest(5)
    void reprocess_shouldProcessAndReturnDetail_whenStatusIsFailed() {
        Transaction transaction = buildTransaction(TransactionStatus.FAILED);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(transactionEventRepository.findByTransactionIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of());

        TransactionEventDetailDTO result = transactionService.reprocess(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(transaction.getStatus()).isIn(
                TransactionStatus.PROCESSED,
                TransactionStatus.FAILED,
                TransactionStatus.RETRY_PENDING
        );
    }

    @Test
    void reprocess_shouldSaveReprocessEventBeforeProcessing() {
        Transaction transaction = buildTransaction(TransactionStatus.FAILED);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(transactionEventRepository.findByTransactionIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of());

        transactionService.reprocess(1L);

        // Al menos 3 eventos: reprocess solicitado + PROCESSING + estado final
        verify(transactionEventRepository, atLeast(3)).save(any(TransactionEvent.class));
    }

    // ===================== helpers =====================

    private Transaction buildTransaction(TransactionStatus status) {
        return Transaction.builder()
                .id(1L)
                .externalId("EXT-001")
                .transactionType(TransactionType.PAYMENT)
                .status(status)
                .sourceSystem("SYSTEM_A")
                .receivedAt(LocalDateTime.of(2026, 6, 7, 10, 0))
                .payload("{\"amount\": 100}")
                .createdAt(LocalDateTime.of(2026, 6, 7, 10, 0, 1))
                .build();
    }
}
