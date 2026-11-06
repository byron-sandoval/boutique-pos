package com.boutique.backend.inventario.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetalleIngresoRequest(
    @NotNull @Positive Long varianteId,
    @NotNull @Positive Integer cantidad,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal costoUnitario
) {
}
