package com.boutique.backend.venta.dto;

import java.math.BigDecimal;

public record DetalleVentaResponse(Long id, Long varianteId, Integer cantidad, BigDecimal precioUnitario,
    BigDecimal descuento, BigDecimal monto) {
}
