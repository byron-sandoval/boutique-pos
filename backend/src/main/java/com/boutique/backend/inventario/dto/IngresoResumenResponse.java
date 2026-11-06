package com.boutique.backend.inventario.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record IngresoResumenResponse(
    Long id,
    Long sucursalId,
    Long proveedorId,
    Long usuarioId,
    Instant fecha,
    String noDocumento,
    BigDecimal total,
    String observaciones,
    Boolean activo
) {
}
