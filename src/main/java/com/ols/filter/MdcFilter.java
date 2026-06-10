package com.ols.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Generar ID corto de 8 caracteres
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        try {
            // Meter el traceId en el MDC para que aparezca en todos los logs
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // Agregar el traceId como header en la respuesta
            response.setHeader(TRACE_ID_HEADER, traceId);

            // Continuar con el request
            filterChain.doFilter(request, response);

        } finally {
            // Limpiar el MDC al terminar el request
            MDC.clear();
        }
    }
}