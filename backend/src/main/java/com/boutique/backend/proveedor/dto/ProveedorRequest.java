package com.boutique.backend.proveedor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
    @NotBlank @Size(max = 100) String nombre,
    @Size(max = 100) String nombreContacto,
    @Size(max = 250) String direccion,
    @Size(max = 20) String telefono,
    @Email @Size(max = 100) String email,
    @NotNull Boolean activo
) {
}
