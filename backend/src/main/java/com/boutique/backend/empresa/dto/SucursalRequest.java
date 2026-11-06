package com.boutique.backend.empresa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SucursalRequest(
    @NotNull @Positive Long empresaId,
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 255) String direccion,
    @Size(max = 20) String telefono,
    @NotNull Boolean activa
) {
}
