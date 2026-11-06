package com.boutique.backend.inventario.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventarioLimitesRequest(
    @NotNull @PositiveOrZero Integer stockMinimo,
    @PositiveOrZero Integer stockMaximo
) {
    @AssertTrue(message = "El stock maximo debe ser mayor o igual al minimo.")
    public boolean isRangoValido() {
        return stockMinimo == null || stockMaximo == null || stockMaximo >= stockMinimo;
    }
}
