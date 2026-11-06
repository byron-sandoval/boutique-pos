package com.boutique.backend.usuario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
    @NotNull @Positive Long sucursalId,
    @NotBlank @Size(max = 20) String cedula,
    @NotBlank @Size(max = 50) String nombre,
    @Size(max = 50) String apellido,
    @Size(max = 20) String telefono,
    @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 50) String username,
    @NotNull RolUsuario rol,
    @NotNull Boolean activo,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank @Size(min = 8, max = 128) String password
) {
    @Override
    public String toString() {
        return "UsuarioCreateRequest[datos reservados]";
    }
}
