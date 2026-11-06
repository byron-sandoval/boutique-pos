package com.boutique.backend.venta.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetalleDevolucionRequest(
    @NotNull @Positive Long detalleVentaId,
    @NotNull @Positive Integer cantidad,
    @NotNull Boolean regresaInventario
) {
}
