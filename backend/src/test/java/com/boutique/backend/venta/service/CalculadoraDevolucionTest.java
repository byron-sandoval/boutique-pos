package com.boutique.backend.venta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.venta.dto.DetalleDevolucionRequest;
import com.boutique.backend.venta.entity.DetalleDevolucion;
import com.boutique.backend.venta.entity.DetalleVenta;
import com.boutique.backend.venta.entity.Venta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraDevolucionTest {
    private final CalculadoraDevolucion calculadora = new CalculadoraDevolucion();

    private BigDecimal monto(String valor) {
        return new BigDecimal(valor);
    }

    private Venta venta(String subtotal, String descuento, String iva, String total, String cambio) {
        Venta venta = new Venta();
        venta.setSubtotal(monto(subtotal));
        venta.setDescuento(monto(descuento));
        venta.setIva(monto(iva));
        venta.setTotal(monto(total));
        venta.setTipoCambioAplicado(monto(cambio));
        return venta;
    }

    private DetalleVenta original(Long id, int cantidad, String precio, String descuento, String neto) {
        VarianteProducto variante = new VarianteProducto();
        variante.setId(id);
        DetalleVenta detalle = new DetalleVenta();
        detalle.setId(id);
        detalle.setVariante(variante);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(monto(precio));
        detalle.setDescuento(monto(descuento));
        detalle.setMonto(monto(neto));
        return detalle;
    }

    private DetalleDevolucion anterior(DetalleVenta original, int cantidad, String total) {
        DetalleDevolucion detalle = new DetalleDevolucion();
        detalle.setDetalleVenta(original);
        detalle.setCantidad(cantidad);
        detalle.setMontoTotal(monto(total));
        return detalle;
    }

    private DetalleDevolucionRequest solicitud(Long id, int cantidad) {
        return new DetalleDevolucionRequest(id, cantidad, true);
    }

    @Test
    void devuelveDescuentoEIvaProporcionalesConLosImportesOriginales() {
        Venta venta = venta("200.00", "20.00", "27.00", "207.00", "1.0000");
        DetalleVenta original = original(1L, 2, "100.00", "20.00", "180.00");
        var r = calculadora.calcular(venta, List.of(original), List.of(), monto("0.00"), List.of(solicitud(1L, 1)));
        assertEquals(monto("103.50"), r.total());
        assertEquals(monto("103.50"), r.importeBase());
        assertEquals(monto("103.50"), r.lineas().getFirst().monto());
    }

    @Test
    void variasDevolucionesParcialesRecuperanExactamenteElTotal() {
        Venta venta = venta("1.50", "0.50", "0.00", "1.00", "1.0000");
        DetalleVenta original = original(1L, 3, "0.50", "0.50", "1.00");
        var primera = calculadora.calcular(venta, List.of(original), List.of(), monto("0.00"), List.of(solicitud(1L, 1)));
        var a = anterior(original, 1, primera.total().toPlainString());
        var segunda = calculadora.calcular(venta, List.of(original), List.of(a), primera.total(), List.of(solicitud(1L, 1)));
        var b = anterior(original, 1, segunda.total().toPlainString());
        var tercera = calculadora.calcular(venta, List.of(original), List.of(a, b), primera.total().add(segunda.total()),
            List.of(solicitud(1L, 1)));
        assertEquals(monto("0.33"), primera.total());
        assertEquals(monto("0.34"), segunda.total());
        assertEquals(monto("0.33"), tercera.total());
        assertEquals(venta.getTotal(), primera.total().add(segunda.total()).add(tercera.total()));
    }

    @Test
    void repartoDeIvaEntreLineasNoInventaCentavos() {
        Venta venta = venta("0.06", "0.00", "0.01", "0.07", "1.0000");
        var a = original(1L, 1, "0.03", "0.00", "0.03");
        var b = original(2L, 1, "0.03", "0.00", "0.03");
        var r = calculadora.calcular(venta, List.of(b, a), List.of(), monto("0.00"),
            List.of(solicitud(2L, 1), solicitud(1L, 1)));
        assertEquals(monto("0.07"), r.total());
        assertEquals(monto("0.03"), r.lineas().get(0).monto());
        assertEquals(monto("0.04"), r.lineas().get(1).monto());
    }

    @Test
    void tipoCambioAcumuladoNoDevuelveMasNioQueLoContabilizado() {
        Venta venta = venta("0.03", "0.00", "0.00", "0.03", "36.5000");
        var original = original(1L, 3, "0.01", "0.00", "0.03");
        var primera = calculadora.calcular(venta, List.of(original), List.of(), monto("0.00"), List.of(solicitud(1L, 1)));
        var a = anterior(original, 1, "0.01");
        var segunda = calculadora.calcular(venta, List.of(original), List.of(a), monto("0.01"), List.of(solicitud(1L, 1)));
        var b = anterior(original, 1, "0.01");
        var tercera = calculadora.calcular(venta, List.of(original), List.of(a, b), monto("0.02"), List.of(solicitud(1L, 1)));
        assertEquals(monto("0.37"), primera.importeBase());
        assertEquals(monto("0.36"), segunda.importeBase());
        assertEquals(monto("0.37"), tercera.importeBase());
        assertEquals(monto("1.10"), primera.importeBase().add(segunda.importeBase()).add(tercera.importeBase()));
    }

    @Test
    void noPermiteSuperarCantidadVendida() {
        Venta venta = venta("100.00", "0.00", "15.00", "115.00", "1.0000");
        var original = original(1L, 1, "100.00", "0.00", "100.00");
        assertThrows(ConflictoException.class, () -> calculadora.calcular(venta, List.of(original), List.of(), monto("0.00"),
            List.of(solicitud(1L, 2))));
    }

    @Test
    void cuentaTambienDevolucionesQueNoRegresaronAlInventario() {
        Venta venta = venta("100.00", "0.00", "15.00", "115.00", "1.0000");
        var original = original(1L, 1, "100.00", "0.00", "100.00");
        var anterior = anterior(original, 1, "115.00");
        anterior.setRegresaInventario(false);
        assertThrows(ConflictoException.class, () -> calculadora.calcular(venta, List.of(original), List.of(anterior),
            monto("115.00"), List.of(solicitud(1L, 1))));
    }

    @Test
    void rechazaDetalleDeOtraVenta() {
        Venta venta = venta("100.00", "0.00", "0.00", "100.00", "1.0000");
        var original = original(1L, 1, "100.00", "0.00", "100.00");
        assertThrows(ConflictoException.class, () -> calculadora.calcular(venta, List.of(original), List.of(), monto("0.00"),
            List.of(solicitud(9L, 1))));
    }

    @Test
    void noAceptaElMismoDetalleDosVecesEnUnaSolicitud() {
        Venta venta = venta("100.00", "0.00", "0.00", "100.00", "1.0000");
        var original = original(1L, 2, "50.00", "0.00", "100.00");
        assertThrows(ConflictoException.class, () -> calculadora.calcular(venta, List.of(original), List.of(), monto("0.00"),
            List.of(solicitud(1L, 1), solicitud(1L, 1))));
    }

    @Test
    void rechazaHistorialConImportesIncompatibles() {
        Venta venta = venta("100.00", "0.00", "0.00", "100.00", "1.0000");
        var original = original(1L, 2, "50.00", "0.00", "100.00");
        var anterior = anterior(original, 1, "60.00");
        assertThrows(ConflictoException.class, () -> calculadora.calcular(venta, List.of(original), List.of(anterior),
            monto("60.00"), List.of(solicitud(1L, 1))));
    }

    @Test
    void validaCabecerasDelHistorialContraSusDetalles() {
        Venta venta = venta("100.00", "0.00", "0.00", "100.00", "1.0000");
        var original = original(1L, 2, "50.00", "0.00", "100.00");
        var anterior = anterior(original, 1, "50.00");
        assertThrows(ConflictoException.class, () -> calculadora.calcular(venta, List.of(original), List.of(anterior),
            monto("40.00"), List.of(solicitud(1L, 1))));
    }

    @Test
    void permiteDevolverProductosConDescuentoCompletoSinReembolsarDinero() {
        Venta venta = venta("100.00", "100.00", "0.00", "0.00", "1.0000");
        var original = original(1L, 1, "100.00", "100.00", "0.00");
        var r = calculadora.calcular(venta, List.of(original), List.of(), monto("0.00"), List.of(solicitud(1L, 1)));
        assertEquals(monto("0.00"), r.total());
        assertEquals(monto("0.00"), r.importeBase());
    }

    @Test
    void devolucionesDeUnaUnidadConservanTotalesParaDistintosPreciosCantidadesYMonedas() {
        for (int centavos = 1; centavos <= 20; centavos++) {
            for (int cantidad = 1; cantidad <= 7; cantidad++) {
                for (String cambio : List.of("1.0000", "36.5000")) {
                    BigDecimal precio = BigDecimal.valueOf(centavos, 2);
                    BigDecimal bruto = precio.multiply(BigDecimal.valueOf(cantidad));
                    BigDecimal descuento = bruto.divide(monto("4"), 2, RoundingMode.HALF_UP);
                    BigDecimal neto = bruto.subtract(descuento);
                    BigDecimal iva = neto.multiply(monto("0.15")).setScale(2, RoundingMode.HALF_UP);
                    Venta venta = venta(bruto.toPlainString(), descuento.toPlainString(), iva.toPlainString(),
                        neto.add(iva).toPlainString(), cambio);
                    DetalleVenta original = original(1L, cantidad, precio.toPlainString(), descuento.toPlainString(), neto.toPlainString());
                    List<DetalleDevolucion> historial = new ArrayList<>();
                    BigDecimal suma = monto("0.00");
                    BigDecimal sumaBase = monto("0.00");
                    for (int unidad = 0; unidad < cantidad; unidad++) {
                        var r = calculadora.calcular(venta, List.of(original), historial, suma, List.of(solicitud(1L, 1)));
                        assertTrue(r.total().signum() >= 0);
                        assertTrue(r.importeBase().signum() >= 0);
                        suma = suma.add(r.total());
                        sumaBase = sumaBase.add(r.importeBase());
                        historial.add(anterior(original, 1, r.total().toPlainString()));
                    }
                    assertEquals(venta.getTotal(), suma);
                    assertEquals(venta.getTotal().multiply(monto(cambio)).setScale(2, RoundingMode.HALF_UP), sumaBase);
                }
            }
        }
    }
}
