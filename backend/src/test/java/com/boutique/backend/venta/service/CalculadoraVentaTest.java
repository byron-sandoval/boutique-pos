package com.boutique.backend.venta.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.boutique.backend.producto.entity.Producto;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.venta.dto.DetalleVentaRequest;
import com.boutique.backend.venta.entity.MetodoPago;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraVentaTest {
    private final CalculadoraVenta calculadora = new CalculadoraVenta();

    private BigDecimal dinero(String valor) {
        return new BigDecimal(valor);
    }

    private Map<Long, VarianteProducto> variantes(String precio) {
        Producto producto = new Producto();
        producto.setPrecio(dinero(precio));
        VarianteProducto variante = new VarianteProducto();
        variante.setId(4L);
        variante.setProducto(producto);
        return Map.of(4L, variante);
    }

    private DetalleVentaRequest linea(int cantidad, String descuento) {
        return new DetalleVentaRequest(4L, cantidad, descuento == null ? null : dinero(descuento));
    }

    @Test
    void aplicaDescuentoAntesDelIvaYCalculaCambio() {
        var r = calculadora.calcular(List.of(linea(2, "20.00")), variantes("100.00"),
            dinero("1.0000"), dinero("15.00"), MetodoPago.EFECTIVO, dinero("250.00"));
        assertEquals(dinero("200.00"), r.subtotal());
        assertEquals(dinero("20.00"), r.descuento());
        assertEquals(dinero("180.00"), r.lineas().getFirst().monto());
        assertEquals(dinero("27.00"), r.iva());
        assertEquals(dinero("207.00"), r.total());
        assertEquals(dinero("43.00"), r.cambio());
        assertEquals(dinero("207.00"), r.totalBase());
    }

    @Test
    void conviertePrecioNioADolaresYAcumuladoDeVueltaANio() {
        var r = calculadora.calcular(List.of(linea(2, "10.00")), variantes("3650.00"),
            dinero("36.5000"), dinero("15.00"), MetodoPago.TARJETA, null);
        assertEquals(dinero("100.00"), r.lineas().getFirst().precioUnitario());
        assertEquals(dinero("200.00"), r.subtotal());
        assertEquals(dinero("28.50"), r.iva());
        assertEquals(dinero("218.50"), r.total());
        assertEquals(dinero("7975.25"), r.totalBase());
        assertEquals(dinero("0.00"), r.cambio());
        assertNull(r.importeRecibido());
    }

    @Test
    void redondeaIvaSobreElNetoDeLaFacturaCompleta() {
        var r = calculadora.calcular(List.of(linea(1, null), linea(1, null)), variantes("0.03"),
            dinero("1.0000"), dinero("15.00"), MetodoPago.TRANSFERENCIA, null);
        assertEquals(dinero("0.06"), r.subtotal());
        assertEquals(dinero("0.01"), r.iva());
        assertEquals(dinero("0.07"), r.total());
    }

    @Test
    void convierteYRedondeaElPrecioUnitarioAntesDeMultiplicarCantidad() {
        var r = calculadora.calcular(List.of(linea(3, null)), variantes("100.00"),
            dinero("3.0000"), dinero("0.00"), MetodoPago.TARJETA, null);
        assertEquals(dinero("33.33"), r.lineas().getFirst().precioUnitario());
        assertEquals(dinero("99.99"), r.total());
        assertEquals(dinero("299.97"), r.totalBase());
    }

    @Test
    void permiteDescuentoCompletoSinGenerarImpuesto() {
        var r = calculadora.calcular(List.of(linea(2, "200.00")), variantes("100.00"),
            dinero("1.0000"), dinero("15.00"), MetodoPago.EFECTIVO, dinero("0.00"));
        assertEquals(dinero("0.00"), r.total());
        assertEquals(dinero("0.00"), r.iva());
    }

    @Test
    void rechazaDescuentoMayorAlBrutoDeLaLinea() {
        assertThrows(ConflictoException.class, () -> calculadora.calcular(List.of(linea(1, "100.01")),
            variantes("100.00"), dinero("1"), dinero("15"), MetodoPago.TARJETA, null));
    }

    @Test
    void rechazaEfectivoInsuficiente() {
        assertThrows(ConflictoException.class, () -> calculadora.calcular(List.of(linea(1, null)),
            variantes("100.00"), dinero("1"), dinero("15"), MetodoPago.EFECTIVO, dinero("114.99")));
    }

    @Test
    void exigeImporteRecibidoParaEfectivo() {
        assertThrows(ConflictoException.class, () -> calculadora.calcular(List.of(linea(1, null)),
            variantes("100.00"), dinero("1"), dinero("15"), MetodoPago.EFECTIVO, null));
    }

    @Test
    void rechazaImporteRecibidoEnTarjeta() {
        assertThrows(ConflictoException.class, () -> calculadora.calcular(List.of(linea(1, null)),
            variantes("100.00"), dinero("1"), dinero("15"), MetodoPago.TARJETA, dinero("115.00")));
    }

    @Test
    void rechazaUnPrecioConvertidoQueSuperaLaColumna() {
        assertThrows(ConflictoException.class, () -> calculadora.calcular(List.of(linea(1, null)),
            variantes("9999999999.99"), dinero("0.0001"), dinero("0"), MetodoPago.TARJETA, null));
    }

    @Test
    void rechazaElDesbordamientoDeUnaLinea() {
        assertThrows(ConflictoException.class, () -> calculadora.calcular(List.of(linea(2, null)),
            variantes("9999999999.99"), dinero("1"), dinero("0"), MetodoPago.TARJETA, null));
    }

    @Test
    void rechazaDesbordamientoDelSubtotalEntreVariasLineas() {
        assertThrows(ConflictoException.class, () -> calculadora.calcular(List.of(linea(1, null), linea(1, null)),
            variantes("6000000000.00"), dinero("1"), dinero("0"), MetodoPago.TARJETA, null));
    }

    @Test
    void rechazaTotalFueraDeRangoDespuesDeSumarIva() {
        assertThrows(ConflictoException.class, () -> calculadora.calcular(List.of(linea(1, null)),
            variantes("9999999999.99"), dinero("1"), dinero("15"), MetodoPago.TARJETA, null));
    }

    @Test
    void noPermiteCobrarCeroPorRedondeoDeUnPrecioPositivo() {
        assertThrows(ConflictoException.class, () -> calculadora.calcular(List.of(linea(1, null)),
            variantes("0.01"), dinero("100"), dinero("15"), MetodoPago.TARJETA, null));
    }
}
