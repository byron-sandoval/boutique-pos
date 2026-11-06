package com.boutique.backend.venta.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DevolucionResumenResponse(Long id, Long ventaId, Long usuarioId, Instant fecha, String motivo,
    BigDecimal total) {
}
