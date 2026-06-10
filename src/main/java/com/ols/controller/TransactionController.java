package com.ols.controller;

import com.ols.dto.TransactionEventDetailDTO;
import com.ols.dto.TransactionFilterDTO;
import com.ols.dto.TransactionRequestDTO;
import com.ols.dto.TransactionResponseDTO;
import com.ols.dto.TransactionSummaryDTO;
import com.ols.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/transactions
     * Registra una nueva transacción enviada por un sistema externo.
     */
    @PostMapping
    public ResponseEntity<TransactionResponseDTO> register(
            @Valid @RequestBody TransactionRequestDTO request) {

        TransactionResponseDTO response = transactionService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    /**
     * GET /api/transactions
     * Lista transacciones con filtros opcionales y paginación.
     */
    @GetMapping
    public ResponseEntity<Page<TransactionSummaryDTO>> findAll(
            @ModelAttribute TransactionFilterDTO filters) { // @ModelAttribute — lee de los parámetros de la URL (GET)

        Page<TransactionSummaryDTO> response = transactionService.findAll(filters);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/transactions/{id}
     * Consulta el detalle de una transacción por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionEventDetailDTO> findById(@PathVariable Long id) {

        TransactionEventDetailDTO response = transactionService.findById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/transactions/{id}/reprocess
     * Reprocesa una transacción fallida manualmente.
     */
    @PostMapping("/{id}/reprocess")
    public ResponseEntity<TransactionEventDetailDTO> reprocess(@PathVariable Long id) {

        TransactionEventDetailDTO response = transactionService.reprocess(id);
        return ResponseEntity.ok(response);
    }
}