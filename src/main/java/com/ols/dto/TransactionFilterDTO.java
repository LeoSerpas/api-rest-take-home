package com.ols.dto;

import com.ols.enums.TransactionStatus;
import com.ols.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionFilterDTO {

    private TransactionStatus status;
    private TransactionType transactionType;
    private String sourceSystem;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTo;

    // Paginación
    private int page = 0;    // Página actual (inicia en 0)
    private int size = 10;   // Cantidad de registros por página
}