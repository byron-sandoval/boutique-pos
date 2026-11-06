package com.boutique.backend.producto.dto;

public record UnidadMedidaResponse(
    Long id,
    String nombre,
    String simbolo,
    Boolean activo
) {
}
