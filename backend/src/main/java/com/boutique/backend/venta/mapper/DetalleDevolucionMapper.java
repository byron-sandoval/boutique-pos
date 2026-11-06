package com.boutique.backend.venta.mapper;

import com.boutique.backend.venta.dto.DetalleDevolucionResponse;
import com.boutique.backend.venta.entity.DetalleDevolucion;
import org.springframework.stereotype.Component;

@Component
public class DetalleDevolucionMapper {
    public DetalleDevolucionResponse toResponse(DetalleDevolucion detalle) {
        return new DetalleDevolucionResponse(detalle.getId(), detalle.getDetalleVenta().getId(), detalle.getCantidad(),
            detalle.getPrecioUnitario(), detalle.getMontoTotal(), detalle.getRegresaInventario());
    }
}
