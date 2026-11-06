package com.boutique.backend.empresa.dto;

public record SucursalResponse(
    Long id,
    Long empresaId,
    String nombre,
    String direccion,
    String telefono,
    Boolean activa
) {
}
