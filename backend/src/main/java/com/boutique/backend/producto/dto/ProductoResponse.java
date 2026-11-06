package com.boutique.backend.producto.dto;

import java.math.BigDecimal;

public record ProductoResponse(
    Long id,
    Long categoriaId,
    Long unidadMedidaId,
    String codigo,
    String nombre,
    String descripcion,
    String marca,
    BigDecimal precio,
    BigDecimal costo,
    BigDecimal ultimoCosto,
    String imagenUrl,
    Boolean activo
) {
}
