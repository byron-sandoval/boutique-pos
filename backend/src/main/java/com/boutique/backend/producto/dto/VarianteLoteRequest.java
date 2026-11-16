package com.boutique.backend.producto.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VarianteLoteRequest(
    @NotNull @Positive Long productoId,
    @NotEmpty @Size(max = 100) List<@NotNull @Valid Fila> variantes
) {
    public record Fila(
        @Size(max = 20) String talla,
        @Size(max = 50) String color,
        @NotBlank @Size(max = 50) String sku,
        @Size(max = 50) String codigoBarras
    ) {
    }
}
