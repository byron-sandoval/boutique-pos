package com.boutique.backend.inventario.dto;

public record InventarioResponse(
    Long id,
    Long varianteId,
    Long sucursalId,
    Integer stock,
    Integer stockMinimo,
    Integer stockMaximo,
    String producto,
    String sku,
    String talla,
    String color,
    String sucursal
) {
}
