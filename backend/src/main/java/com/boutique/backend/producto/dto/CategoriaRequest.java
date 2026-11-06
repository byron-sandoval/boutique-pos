package com.boutique.backend.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
    @NotBlank @Size(max = 50) String nombre,
    @Size(max = 255) String descripcion,
    @NotNull Boolean activo
) {
}
