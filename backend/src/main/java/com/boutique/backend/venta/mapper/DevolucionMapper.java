package com.boutique.backend.venta.mapper;

import java.util.List;

import com.boutique.backend.venta.dto.DetalleDevolucionResponse;
import com.boutique.backend.venta.dto.DevolucionResponse;
import com.boutique.backend.venta.dto.DevolucionResumenResponse;
import com.boutique.backend.venta.entity.Devolucion;
import org.springframework.stereotype.Component;

@Component
public class DevolucionMapper {
    public DevolucionResumenResponse toResumen(Devolucion devolucion) {
        return new DevolucionResumenResponse(devolucion.getId(), devolucion.getVenta().getId(),
            devolucion.getUsuario().getId(), devolucion.getFecha(), devolucion.getMotivo(), devolucion.getTotal());
    }

    public DevolucionResponse toResponse(Devolucion devolucion, List<DetalleDevolucionResponse> detalles) {
        var r = toResumen(devolucion);
        return new DevolucionResponse(r.id(), r.ventaId(), r.usuarioId(), r.fecha(), r.motivo(), r.total(), detalles);
    }
}
