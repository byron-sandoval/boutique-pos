package com.boutique.backend.inventario.dto;

public record InventarioResponse(
    Long id,
    Long varianteId,
    Long sucursalId,
    Integer stock,
    Integer stockMinimo,
    Integer stockMaximo
) {
}
