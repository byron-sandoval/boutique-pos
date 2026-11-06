package com.boutique.backend.inventario.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.inventario.dto.DetalleIngresoResponse;
import com.boutique.backend.inventario.dto.IngresoRequest;
import com.boutique.backend.inventario.dto.IngresoResponse;
import com.boutique.backend.inventario.dto.IngresoResumenResponse;
import com.boutique.backend.inventario.entity.Ingreso;
import com.boutique.backend.proveedor.entity.Proveedor;
import com.boutique.backend.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class IngresoMapper {

    public Ingreso toEntity(IngresoRequest request, Sucursal sucursal, Usuario usuario,
        Proveedor proveedor, BigDecimal total) {
        Ingreso ingreso = new Ingreso();
        ingreso.setSucursal(sucursal);
        ingreso.setUsuario(usuario);
        ingreso.setProveedor(proveedor);
        ingreso.setNoDocumento(request.noDocumento().trim());
        ingreso.setObservaciones(request.observaciones());
        ingreso.setTotal(total);
        return ingreso;
    }

    public IngresoResumenResponse toResumen(Ingreso ingreso) {
        return new IngresoResumenResponse(ingreso.getId(), ingreso.getSucursal().getId(),
            ingreso.getProveedor() == null ? null : ingreso.getProveedor().getId(), ingreso.getUsuario().getId(),
            ingreso.getFecha(), ingreso.getNoDocumento(), ingreso.getTotal(), ingreso.getObservaciones(), ingreso.getActivo());
    }

    public IngresoResponse toResponse(Ingreso ingreso, List<DetalleIngresoResponse> detalles) {
        IngresoResumenResponse resumen = toResumen(ingreso);
        return new IngresoResponse(resumen.id(), resumen.sucursalId(), resumen.proveedorId(), resumen.usuarioId(),
            resumen.fecha(), resumen.noDocumento(), resumen.total(), resumen.observaciones(), resumen.activo(), detalles);
    }
}
