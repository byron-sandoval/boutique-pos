package com.boutique.backend.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
    @Size(max = 20) String cedula,
    @Size(max = 30) String pasaporte,
    @NotBlank @Size(max = 100) String nombre,
    @Size(max = 255) String direccion,
    @Size(max = 20) String telefono,
    @NotNull Boolean activo
) {
}
