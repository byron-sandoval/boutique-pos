package com.boutique.backend.proveedor.dto;

public record ProveedorResponse(
    Long id,
    String nombre,
    String nombreContacto,
    String direccion,
    String telefono,
    String email,
    Boolean activo
) {
}
