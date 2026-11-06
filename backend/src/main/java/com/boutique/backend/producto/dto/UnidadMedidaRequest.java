package com.boutique.backend.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UnidadMedidaRequest(
    @NotBlank @Size(max = 30) String nombre,
    @Size(max = 10) String simbolo,
    @NotNull Boolean activo
) {
}
