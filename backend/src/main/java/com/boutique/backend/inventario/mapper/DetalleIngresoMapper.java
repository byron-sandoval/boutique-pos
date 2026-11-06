package com.boutique.backend.inventario.mapper;

import java.math.BigDecimal;

import com.boutique.backend.inventario.dto.DetalleIngresoRequest;
import com.boutique.backend.inventario.dto.DetalleIngresoResponse;
import com.boutique.backend.inventario.entity.DetalleIngreso;
import com.boutique.backend.inventario.entity.Ingreso;
import com.boutique.backend.producto.entity.VarianteProducto;
import org.springframework.stereotype.Component;

@Component
public class DetalleIngresoMapper {

    public DetalleIngreso toEntity(DetalleIngresoRequest request, Ingreso ingreso,
        VarianteProducto variante, BigDecimal monto) {
        DetalleIngreso detalle = new DetalleIngreso();
        detalle.setIngreso(ingreso);
        detalle.setVariante(variante);
        detalle.setCantidad(request.cantidad());
        detalle.setCostoUnitario(request.costoUnitario());
        detalle.setMonto(monto);
        return detalle;
    }

    public DetalleIngresoResponse toResponse(DetalleIngreso detalle) {
        return new DetalleIngresoResponse(detalle.getId(), detalle.getVariante().getId(),
            detalle.getCantidad(), detalle.getCostoUnitario(), detalle.getMonto());
    }
}
