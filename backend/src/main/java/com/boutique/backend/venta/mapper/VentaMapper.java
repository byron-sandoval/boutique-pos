package com.boutique.backend.venta.mapper;

import java.util.List;

import com.boutique.backend.venta.dto.DetalleVentaResponse;
import com.boutique.backend.venta.dto.VentaResponse;
import com.boutique.backend.venta.dto.VentaResumenResponse;
import com.boutique.backend.venta.entity.Venta;
import org.springframework.stereotype.Component;

@Component
public class VentaMapper {

    public VentaResumenResponse toResumen(Venta venta) {
        return new VentaResumenResponse(venta.getId(), venta.getSucursal().getId(), venta.getUsuario().getId(),
            venta.getCliente() == null ? null : venta.getCliente().getId(), venta.getMoneda().getId(),
            venta.getSerieFactura().getId(), venta.getSesionCaja().getId(), venta.getNoFactura(), venta.getNumeroFactura(),
            venta.getFecha(), venta.getSubtotal(), venta.getDescuento(), venta.getIva(), venta.getTotal(),
            venta.getMetodoPago(), venta.getTipoCambioAplicado(), venta.getImporteRecibido(), venta.getCambio(),
            venta.getEstado(), venta.getMotivoAnulacion(), venta.getAnuladaEn(),
            venta.getAnuladaPor() == null ? null : venta.getAnuladaPor().getId());
    }

    public VentaResponse toResponse(Venta venta, List<DetalleVentaResponse> detalles) {
        VentaResumenResponse r = toResumen(venta);
        return new VentaResponse(r.id(), r.sucursalId(), r.usuarioId(), r.clienteId(), r.monedaId(), r.serieFacturaId(),
            r.sesionCajaId(), r.noFactura(), r.numeroFactura(), r.fecha(), r.subtotal(), r.descuento(), r.iva(), r.total(),
            r.metodoPago(), r.tipoCambioAplicado(), r.importeRecibido(), r.cambio(), r.estado(), r.motivoAnulacion(),
            r.anuladaEn(), r.anuladaPorId(), detalles);
    }
}
