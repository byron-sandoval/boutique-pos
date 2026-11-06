package com.boutique.backend.venta.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DevolucionResponse(Long id, Long ventaId, Long usuarioId, Instant fecha, String motivo,
    BigDecimal total, List<DetalleDevolucionResponse> detalles) {
}
