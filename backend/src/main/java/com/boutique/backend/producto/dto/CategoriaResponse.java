package com.boutique.backend.producto.dto;

public record CategoriaResponse(
    Long id,
    String nombre,
    String descripcion,
    Boolean activo
) {
}
