package com.boutique.backend.venta.mapper;

import com.boutique.backend.venta.dto.DetalleVentaResponse;
import com.boutique.backend.venta.entity.DetalleVenta;
import org.springframework.stereotype.Component;

@Component
public class DetalleVentaMapper {
    public DetalleVentaResponse toResponse(DetalleVenta detalle) {
        return new DetalleVentaResponse(detalle.getId(), detalle.getVariante().getId(), detalle.getCantidad(),
            detalle.getPrecioUnitario(), detalle.getDescuento(), detalle.getMonto());
    }
}
