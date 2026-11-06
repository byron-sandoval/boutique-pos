package com.boutique.backend.caja.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SesionCajaCierreRequest(
    @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal efectivoCierre,
    @Size(max = 5000) String notas
) {
}
