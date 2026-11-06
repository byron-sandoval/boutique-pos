package com.boutique.backend.cliente.dto;

public record ClienteResponse(
    Long id,
    String cedula,
    String pasaporte,
    String nombre,
    String direccion,
    String telefono,
    Boolean activo
) {
}
