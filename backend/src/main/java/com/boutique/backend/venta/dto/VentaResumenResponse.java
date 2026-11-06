package com.boutique.backend.venta.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.boutique.backend.venta.entity.EstadoVenta;
import com.boutique.backend.venta.entity.MetodoPago;

public record VentaResumenResponse(Long id, Long sucursalId, Long usuarioId, Long clienteId, Long monedaId,
    Long serieFacturaId, Long sesionCajaId, Long noFactura, String numeroFactura, Instant fecha,
    BigDecimal subtotal, BigDecimal descuento, BigDecimal iva, BigDecimal total, MetodoPago metodoPago,
    BigDecimal tipoCambioAplicado, BigDecimal importeRecibido, BigDecimal cambio, EstadoVenta estado,
    String motivoAnulacion, Instant anuladaEn, Long anuladaPorId) {
}
