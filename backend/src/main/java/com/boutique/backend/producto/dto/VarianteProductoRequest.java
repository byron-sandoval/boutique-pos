package com.boutique.backend.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VarianteProductoRequest(
    @NotNull @Positive Long productoId,
    @Size(max = 20) String talla,
    @Size(max = 50) String color,
    @NotBlank @Size(max = 50) String sku,
    @Size(max = 50) String codigoBarras,
    @NotNull Boolean activo
) {
}
