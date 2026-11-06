package com.boutique.backend.venta.dto;

import java.math.BigDecimal;

public record DetalleDevolucionResponse(Long id, Long detalleVentaId, Integer cantidad, BigDecimal precioUnitario,
    BigDecimal montoTotal, Boolean regresaInventario) {
}
