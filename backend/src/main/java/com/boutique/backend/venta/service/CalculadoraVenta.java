package com.boutique.backend.venta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.venta.dto.DetalleVentaRequest;
import com.boutique.backend.venta.entity.MetodoPago;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraVenta {

    private static final BigDecimal MAXIMO = new BigDecimal("9999999999.99");
    private static final BigDecimal CERO = new BigDecimal("0.00");

    // Los precios del catálogo están en NIO y no incluyen IVA.
    // El tipo de cambio expresa NIO por una unidad de la moneda facturada.
    public Calculo calcular(List<DetalleVentaRequest> solicitudes, Map<Long, VarianteProducto> variantes,
        BigDecimal tipoCambio, BigDecimal porcentajeIva, MetodoPago metodoPago, BigDecimal importeRecibido) {
        BigDecimal subtotal = CERO;
        BigDecimal descuento = CERO;
        List<LineaCalculada> lineas = new ArrayList<>();
        for (DetalleVentaRequest solicitud : solicitudes) {
            VarianteProducto variante = variantes.get(solicitud.varianteId());
            BigDecimal precioBase = variante.getProducto().getPrecio();
            BigDecimal precio = precioBase.divide(tipoCambio, 2, RoundingMode.HALF_UP);
            validarMonto(precio);
            if (precioBase.signum() > 0 && precio.signum() == 0) {
                throw new ConflictoException("El tipo de cambio hace que el precio unitario se redondee a cero.");
            }
            BigDecimal bruto = precio.multiply(BigDecimal.valueOf(solicitud.cantidad()));
            validarMonto(bruto);
            BigDecimal rebaja = solicitud.descuento() == null ? CERO : solicitud.descuento().setScale(2, RoundingMode.UNNECESSARY);
            validarMonto(rebaja);
            if (rebaja.compareTo(bruto) > 0) {
                throw new ConflictoException("El descuento no puede superar el importe de la línea.");
            }
            BigDecimal neto = bruto.subtract(rebaja);
            lineas.add(new LineaCalculada(variante, solicitud.cantidad(), precio, rebaja, neto));
            subtotal = subtotal.add(bruto);
            descuento = descuento.add(rebaja);
            validarMonto(subtotal);
            validarMonto(descuento);
        }
        BigDecimal neto = subtotal.subtract(descuento);
        BigDecimal iva = neto.multiply(porcentajeIva).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal total = neto.add(iva);
        validarMonto(iva);
        validarMonto(total);
        BigDecimal cambio = CERO;
        BigDecimal recibido = null;
        if (metodoPago == MetodoPago.EFECTIVO) {
            if (importeRecibido == null || importeRecibido.compareTo(total) < 0) {
                throw new ConflictoException("El importe recibido en efectivo debe cubrir el total de la venta.");
            }
            recibido = importeRecibido.setScale(2, RoundingMode.UNNECESSARY);
            validarMonto(recibido);
            cambio = recibido.subtract(total);
        } else if (importeRecibido != null) {
            throw new ConflictoException("El importe recibido y el cambio se registran solo para pagos en efectivo.");
        }
        BigDecimal totalBase = total.multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP);
        return new Calculo(subtotal, descuento, iva, total, recibido, cambio, totalBase, List.copyOf(lineas));
    }

    private void validarMonto(BigDecimal monto) {
        if (monto.signum() < 0 || monto.compareTo(MAXIMO) > 0) {
            throw new ConflictoException("Un importe de la venta supera el rango permitido por la base de datos.");
        }
    }

    public record LineaCalculada(VarianteProducto variante, int cantidad, BigDecimal precioUnitario,
        BigDecimal descuento, BigDecimal monto) {
    }

    public record Calculo(BigDecimal subtotal, BigDecimal descuento, BigDecimal iva, BigDecimal total,
        BigDecimal importeRecibido, BigDecimal cambio, BigDecimal totalBase, List<LineaCalculada> lineas) {
    }
}
