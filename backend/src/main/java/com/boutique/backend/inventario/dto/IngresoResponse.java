package com.boutique.backend.inventario.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record IngresoResponse(
    Long id,
    Long sucursalId,
    Long proveedorId,
    Long usuarioId,
    Instant fecha,
    String noDocumento,
    BigDecimal total,
    String observaciones,
    Boolean activo,
    List<DetalleIngresoResponse> detalles
) {
}
