package com.boutique.backend.venta.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetalleVentaRequest(
    @NotNull @Positive Long varianteId,
    @NotNull @Positive Integer cantidad,
    // Importe de descuento de toda la línea, en la moneda de la venta; null equivale a cero.
    @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal descuento
) {
}
