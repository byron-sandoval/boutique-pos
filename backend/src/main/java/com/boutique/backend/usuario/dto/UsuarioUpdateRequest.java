package com.boutique.backend.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
    @NotNull @Positive Long sucursalId,
    @NotBlank @Size(max = 20) String cedula,
    @NotBlank @Size(max = 50) String nombre,
    @Size(max = 50) String apellido,
    @Size(max = 20) String telefono,
    @Email @Size(max = 254) String email
) {
}
