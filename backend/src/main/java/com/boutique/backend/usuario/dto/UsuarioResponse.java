package com.boutique.backend.usuario.dto;

public record UsuarioResponse(
    Long id,
    String keycloakId,
    Long sucursalId,
    String cedula,
    String nombre,
    String apellido,
    String telefono,
    String email,
    String username,
    Boolean activo
) {
}
