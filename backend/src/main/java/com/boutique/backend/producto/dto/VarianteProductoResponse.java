package com.boutique.backend.producto.dto;

public record VarianteProductoResponse(
    Long id,
    Long productoId,
    String talla,
    String color,
    String sku,
    String codigoBarras,
    Boolean activo
) {
}
