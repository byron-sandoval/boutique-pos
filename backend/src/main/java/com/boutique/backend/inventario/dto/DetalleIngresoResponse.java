package com.boutique.backend.inventario.dto;

import java.math.BigDecimal;

public record DetalleIngresoResponse(
    Long id,
    Long varianteId,
    Integer cantidad,
    BigDecimal costoUnitario,
    BigDecimal monto
) {
}
