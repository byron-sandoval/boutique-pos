package com.boutique.backend.venta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.venta.dto.DetalleDevolucionRequest;
import com.boutique.backend.venta.entity.DetalleDevolucion;
import com.boutique.backend.venta.entity.DetalleVenta;
import com.boutique.backend.venta.entity.Venta;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraDevolucion {

    private static final BigDecimal CERO = new BigDecimal("0.00");

    public Calculo calcular(Venta venta, List<DetalleVenta> originales, List<DetalleDevolucion> anteriores,
        BigDecimal totalAnterior, List<DetalleDevolucionRequest> solicitudes) {
        if (originales.isEmpty()) {
            throw conflicto("La venta no tiene detalles.");
        }
        Map<Long, DetalleVenta> porId = new HashMap<>();
        BigDecimal neto = CERO;
        for (DetalleVenta detalle : originales) {
            if (detalle.getCantidad() <= 0 || detalle.getMonto().signum() < 0
                || porId.put(detalle.getId(), detalle) != null) {
                throw conflicto("Los detalles originales de la venta no son válidos.");
            }
            neto = neto.add(detalle.getMonto());
        }
        if (neto.compareTo(venta.getSubtotal().subtract(venta.getDescuento())) != 0
            || neto.add(venta.getIva()).compareTo(venta.getTotal()) != 0
            || (neto.signum() == 0 && venta.getIva().signum() != 0)) {
            throw conflicto("Los importes de la factura no coinciden con sus detalles.");
        }
        // Reparto acumulado del IVA: la última línea absorbe el centavo restante, sin cambiar la factura.
        Map<Long, BigDecimal> totalPorLinea = new HashMap<>();
        BigDecimal netoAcumulado = CERO;
        BigDecimal ivaAnterior = CERO;
        for (DetalleVenta detalle : originales.stream().sorted(Comparator.comparing(DetalleVenta::getId)).toList()) {
            netoAcumulado = netoAcumulado.add(detalle.getMonto());
            BigDecimal ivaAcumulado = neto.signum() == 0 ? CERO
                : venta.getIva().multiply(netoAcumulado).divide(neto, 2, RoundingMode.HALF_UP);
            totalPorLinea.put(detalle.getId(), detalle.getMonto().add(ivaAcumulado.subtract(ivaAnterior)));
            ivaAnterior = ivaAcumulado;
        }

        Map<Long, Long> cantidadesAnteriores = new HashMap<>();
        Map<Long, BigDecimal> montosAnteriores = new HashMap<>();
        BigDecimal sumaAnterior = CERO;
        for (DetalleDevolucion detalle : anteriores) {
            Long id = detalle.getDetalleVenta().getId();
            if (!porId.containsKey(id) || detalle.getCantidad() <= 0 || detalle.getMontoTotal().signum() < 0) {
                throw conflicto("El historial de devoluciones no coincide con la venta.");
            }
            cantidadesAnteriores.merge(id, detalle.getCantidad().longValue(), Long::sum);
            montosAnteriores.merge(id, detalle.getMontoTotal(), BigDecimal::add);
            sumaAnterior = sumaAnterior.add(detalle.getMontoTotal());
        }
        if (sumaAnterior.compareTo(totalAnterior) != 0) {
            throw conflicto("El total del historial de devoluciones no coincide con sus detalles.");
        }
        for (DetalleVenta original : originales) {
            long cantidad = cantidadesAnteriores.getOrDefault(original.getId(), 0L);
            BigDecimal monto = montosAnteriores.getOrDefault(original.getId(), CERO);
            if (cantidad > original.getCantidad()
                || monto.compareTo(proporcion(totalPorLinea.get(original.getId()), cantidad, original.getCantidad())) != 0) {
                throw conflicto("El historial contiene cantidades o importes incompatibles con la venta.");
            }
        }

        List<LineaCalculada> lineas = new ArrayList<>();
        Set<Long> solicitados = new HashSet<>();
        BigDecimal total = CERO;
        for (DetalleDevolucionRequest solicitud : solicitudes) {
            Long id = solicitud.detalleVentaId();
            DetalleVenta original = porId.get(id);
            if (original == null) {
                throw conflicto("Uno de los detalles solicitados no pertenece a la venta.");
            }
            if (!solicitados.add(id)) {
                throw conflicto("No repitas el mismo detalle de venta en una devolución.");
            }
            long cantidadPrevia = cantidadesAnteriores.getOrDefault(id, 0L);
            long cantidadFinal = cantidadPrevia + solicitud.cantidad();
            if (solicitud.cantidad() <= 0 || cantidadFinal > original.getCantidad()) {
                throw conflicto("La cantidad solicitada supera las unidades pendientes de devolver.");
            }
            BigDecimal montoAcumulado = proporcion(totalPorLinea.get(id), cantidadFinal, original.getCantidad());
            BigDecimal monto = montoAcumulado.subtract(montosAnteriores.getOrDefault(id, CERO));
            lineas.add(new LineaCalculada(original, solicitud.cantidad(), solicitud.regresaInventario(), monto));
            total = total.add(monto);
        }
        if (totalAnterior.add(total).compareTo(venta.getTotal()) > 0) {
            throw conflicto("La devolución supera el importe pendiente de la venta.");
        }
        // Diferencia de importes acumulados: evita sumar un centavo de más en varias devoluciones en USD.
        BigDecimal baseAnterior = totalAnterior.multiply(venta.getTipoCambioAplicado()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseFinal = totalAnterior.add(total).multiply(venta.getTipoCambioAplicado()).setScale(2, RoundingMode.HALF_UP);
        return new Calculo(total, baseFinal.subtract(baseAnterior), List.copyOf(lineas));
    }

    private BigDecimal proporcion(BigDecimal total, long cantidad, int cantidadVendida) {
        return total.multiply(BigDecimal.valueOf(cantidad)).divide(BigDecimal.valueOf(cantidadVendida), 2, RoundingMode.HALF_UP);
    }

    private ConflictoException conflicto(String mensaje) {
        return new ConflictoException(mensaje);
    }

    public record LineaCalculada(DetalleVenta original, int cantidad, boolean regresaInventario, BigDecimal monto) {
    }

    public record Calculo(BigDecimal total, BigDecimal importeBase, List<LineaCalculada> lineas) {
    }
}
