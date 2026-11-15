package com.boutique.backend.inventario.dto;

import java.math.BigDecimal;

public record DetalleIngresoResponse(
    Long id,
    Long varianteId,
    String producto,
    String sku,
    String talla,
    String color,
    Integer cantidad,
    BigDecimal costoUnitario,
    BigDecimal monto
) {
}
