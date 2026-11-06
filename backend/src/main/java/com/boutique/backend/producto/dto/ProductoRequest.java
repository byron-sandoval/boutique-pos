package com.boutique.backend.producto.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
    @NotNull @Positive Long categoriaId,
    @NotNull @Positive Long unidadMedidaId,
    @NotBlank @Size(max = 50) String codigo,
    @NotBlank @Size(max = 100) String nombre,
    @Size(max = 255) String descripcion,
    @Size(max = 100) String marca,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precio,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal costo,
    @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal ultimoCosto,
    @Size(max = 500) String imagenUrl,
    @NotNull Boolean activo
) {
}
