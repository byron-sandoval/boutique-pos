package com.boutique.backend.venta.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.boutique.backend.caja.entity.SesionCaja;
import com.boutique.backend.caja.service.CajaVentasService;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.inventario.service.StockService;
import com.boutique.backend.moneda.entity.Moneda;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import com.boutique.backend.venta.dto.VentaAnulacionRequest;
import com.boutique.backend.venta.entity.DetalleVenta;
import com.boutique.backend.venta.entity.EstadoVenta;
import com.boutique.backend.venta.entity.MetodoPago;
import com.boutique.backend.venta.entity.SerieFactura;
import com.boutique.backend.venta.entity.Venta;
import com.boutique.backend.venta.mapper.DetalleVentaMapper;
import com.boutique.backend.venta.mapper.VentaMapper;
import com.boutique.backend.venta.repository.DetalleVentaRepository;
import com.boutique.backend.venta.repository.DevolucionRepository;
import com.boutique.backend.venta.repository.VentaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaAnulacionServiceTest {
    @Mock VentaRepository ventas;
    @Mock DetalleVentaRepository detalles;
    @Mock DevolucionRepository devoluciones;
    @Mock UsuarioRepository usuarios;
    @Mock CajaVentasService cajas;
    @Mock StockService stock;
    private VentaAnulacionService service;
    private Usuario usuario;
    private SesionCaja caja;
    private Venta venta;
    private SerieFactura serie;

    @BeforeEach
    void preparar() {
        service = new VentaAnulacionService(ventas, detalles, devoluciones, usuarios, cajas, stock,
            new VentaMapper(), new DetalleVentaMapper());
        Sucursal sucursal = new Sucursal();
        sucursal.setId(2L);
        usuario = new Usuario();
        usuario.setId(7L);
        usuario.setSucursal(sucursal);
        caja = new SesionCaja();
        caja.setId(5L);
        caja.setSucursal(sucursal);
        caja.setAbiertoPor(usuario);
        serie = new SerieFactura();
        serie.setId(3L);
        serie.setSiguienteNumero(9L);
        Moneda moneda = new Moneda();
        moneda.setId(1L);
        venta = new Venta();
        venta.setId(10L);
        venta.setSucursal(sucursal);
        venta.setUsuario(usuario);
        venta.setMoneda(moneda);
        venta.setSerieFactura(serie);
        venta.setSesionCaja(caja);
        venta.setNoFactura(8L);
        venta.setNumeroFactura("FAC-000008");
        venta.setTotal(new BigDecimal("207.00"));
        venta.setTipoCambioAplicado(new BigDecimal("1.0000"));
        venta.setMetodoPago(MetodoPago.EFECTIVO);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar() {
        Jwt jwt = Jwt.withTokenValue("prueba").header("alg", "RS256").subject("keycloak-7").build();
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(usuarios.findByKeycloakId("keycloak-7")).thenReturn(Optional.of(usuario));
    }

    private void prepararVentaBloqueada() {
        autenticar();
        when(ventas.findSesionCajaId(10L)).thenReturn(Optional.of(5L));
        when(cajas.obtenerParaAnulacion(5L, 2L)).thenReturn(caja);
        when(ventas.findByIdForUpdate(10L)).thenReturn(Optional.of(venta));
    }

    private DetalleVenta detalle(Long varianteId, int cantidad) {
        VarianteProducto variante = new VarianteProducto();
        variante.setId(varianteId);
        DetalleVenta detalle = new DetalleVenta();
        detalle.setVenta(venta);
        detalle.setVariante(variante);
        detalle.setCantidad(cantidad);
        return detalle;
    }

    private VentaAnulacionRequest motivo() {
        return new VentaAnulacionRequest(" Error de facturación ");
    }

    @Test
    void conservaFacturaImportesYContadorAlAnularYRegistraAuditoria() {
        prepararVentaBloqueada();
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(4L, 2)));
        when(ventas.saveAndFlush(venta)).thenReturn(venta);
        var r = service.anular(10L, motivo());
        assertEquals(EstadoVenta.ANULADA, r.estado());
        assertEquals("Error de facturación", r.motivoAnulacion());
        assertEquals(7L, r.anuladaPorId());
        assertNotNull(r.anuladaEn());
        assertEquals("FAC-000008", r.numeroFactura());
        assertEquals(8L, r.noFactura());
        assertEquals(9L, serie.getSiguienteNumero());
        assertEquals(new BigDecimal("207.00"), r.total());
        verify(cajas).anularVenta(caja, new BigDecimal("207.00"), MetodoPago.EFECTIVO);
        verify(stock).ingresar(4L, 2L, 2);
        verify(ventas, never()).delete(any());
        verify(detalles, never()).deleteAll(anyList());
    }

    @Test
    void calculaReversionEnNioConTipoCambioHistoricoYRedondeoOriginal() {
        prepararVentaBloqueada();
        venta.setTotal(new BigDecimal("3.15"));
        venta.setTipoCambioAplicado(new BigDecimal("36.5000"));
        venta.getMoneda().setActivo(false);
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(4L, 1)));
        when(ventas.saveAndFlush(venta)).thenReturn(venta);
        service.anular(10L, motivo());
        verify(cajas).anularVenta(caja, new BigDecimal("114.98"), MetodoPago.EFECTIVO);
    }

    @Test
    void reintegraVariantesRepetidasUnaSolaVezEnOrdenEstable() {
        prepararVentaBloqueada();
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(9L, 1), detalle(4L, 2), detalle(9L, 3)));
        when(ventas.saveAndFlush(venta)).thenReturn(venta);
        service.anular(10L, motivo());
        var orden = inOrder(stock);
        orden.verify(stock).ingresar(4L, 2L, 2);
        orden.verify(stock).ingresar(9L, 2L, 4);
        verifyNoMoreInteractions(stock);
    }

    @Test
    void ventaYaAnuladaNoRepiteMovimientosNiCambiaAuditoria() {
        prepararVentaBloqueada();
        venta.setEstado(EstadoVenta.ANULADA);
        venta.setMotivoAnulacion("Motivo original");
        Instant fecha = Instant.parse("2026-11-01T12:00:00Z");
        venta.setAnuladaEn(fecha);
        venta.setAnuladaPor(usuario);
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(4L, 2)));
        var r = service.anular(10L, motivo());
        assertEquals("Motivo original", r.motivoAnulacion());
        assertEquals(fecha, r.anuladaEn());
        verify(cajas, never()).anularVenta(any(), any(), any());
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(stock, devoluciones);
    }

    @Test
    void dosLlamadasSoloAnulanUnaVez() {
        prepararVentaBloqueada();
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(4L, 2)));
        when(ventas.saveAndFlush(venta)).thenReturn(venta);
        service.anular(10L, motivo());
        service.anular(10L, motivo());
        verify(stock, times(1)).ingresar(4L, 2L, 2);
        verify(cajas, times(1)).anularVenta(caja, new BigDecimal("207.00"), MetodoPago.EFECTIVO);
        verify(ventas, times(1)).saveAndFlush(venta);
    }

    @Test
    void exigeUsuarioAutenticado() {
        assertThrows(AccessDeniedException.class, () -> service.anular(10L, motivo()));
        verifyNoInteractions(ventas, cajas, stock);
    }

    @Test
    void exigeUsuarioActivo() {
        autenticar();
        usuario.setActivo(false);
        assertThrows(AccessDeniedException.class, () -> service.anular(10L, motivo()));
        verifyNoInteractions(ventas, cajas, stock);
    }

    @Test
    void exigeUsuarioRegistrado() {
        autenticar();
        when(usuarios.findByKeycloakId("keycloak-7")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.anular(10L, motivo()));
        verifyNoInteractions(ventas, cajas, stock);
    }

    @Test
    void informaVentaInexistente() {
        autenticar();
        when(ventas.findSesionCajaId(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.anular(99L, motivo()));
        verifyNoInteractions(cajas, stock);
    }

    @Test
    void rechazoDeCajaCerradaImpideReintegrarStock() {
        autenticar();
        when(ventas.findSesionCajaId(10L)).thenReturn(Optional.of(5L));
        when(cajas.obtenerParaAnulacion(5L, 2L)).thenThrow(new ConflictoException("Caja cerrada"));
        assertThrows(ConflictoException.class, () -> service.anular(10L, motivo()));
        verify(ventas, never()).findByIdForUpdate(any());
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(stock);
    }

    @Test
    void noAnulaVentasDeOtraSucursal() {
        prepararVentaBloqueada();
        Sucursal otra = new Sucursal();
        otra.setId(6L);
        venta.setSucursal(otra);
        assertThrows(AccessDeniedException.class, () -> service.anular(10L, motivo()));
        verifyNoInteractions(stock, detalles, devoluciones);
        verify(ventas, never()).saveAndFlush(any());
    }

    @Test
    void devolucionesExistentesBloqueanLaAnulacion() {
        prepararVentaBloqueada();
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(4L, 2)));
        when(devoluciones.existsByVentaId(10L)).thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.anular(10L, motivo()));
        verifyNoInteractions(stock);
        verify(cajas, never()).anularVenta(any(), any(), any());
        verify(ventas, never()).saveAndFlush(any());
    }

    @Test
    void rechazaVentaSinDetalles() {
        prepararVentaBloqueada();
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of());
        assertThrows(ConflictoException.class, () -> service.anular(10L, motivo()));
        verifyNoInteractions(stock);
        verify(cajas, never()).anularVenta(any(), any(), any());
    }

    @Test
    void rechazaDesbordamientoDeCantidadesAntesDeModificarCaja() {
        prepararVentaBloqueada();
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(4L, Integer.MAX_VALUE), detalle(4L, 1)));
        assertThrows(ConflictoException.class, () -> service.anular(10L, motivo()));
        verifyNoInteractions(stock);
        verify(cajas, never()).anularVenta(any(), any(), any());
    }

    @Test
    void falloDeStockNoMarcaLaVentaComoAnulada() {
        prepararVentaBloqueada();
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(4L, 2)));
        doThrow(new ConflictoException("Stock fuera de rango")).when(stock).ingresar(4L, 2L, 2);
        assertThrows(ConflictoException.class, () -> service.anular(10L, motivo()));
        assertEquals(EstadoVenta.COMPLETADA, venta.getEstado());
        assertNull(venta.getAnuladaEn());
        verify(ventas, never()).saveAndFlush(any());
    }

    @Test
    void adminPuedeAnularVentaDeUnCajeroSinCambiarElVendedorNiElDuenoDeCaja() {
        prepararVentaBloqueada();
        Usuario cajero = new Usuario();
        cajero.setId(9L);
        cajero.setSucursal(usuario.getSucursal());
        caja.setAbiertoPor(cajero);
        venta.setUsuario(cajero);
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(4L, 2)));
        when(ventas.saveAndFlush(venta)).thenReturn(venta);

        var r = service.anular(10L, motivo());

        assertEquals(EstadoVenta.ANULADA, r.estado());
        assertEquals(9L, r.usuarioId());
        assertEquals(7L, r.anuladaPorId());
        assertSame(cajero, caja.getAbiertoPor());
        verify(cajas).obtenerParaAnulacion(5L, 2L);
        verify(cajas, never()).obtenerParaVenta(any(), any(), any());
        verify(cajas).anularVenta(caja, new BigDecimal("207.00"), MetodoPago.EFECTIVO);
        verify(stock).ingresar(4L, 2L, 2);
    }

    @Test
    void cajeroNoPuedeAnularNiSuPropiaVentaDesdeElServicio() {
        Jwt jwt = Jwt.withTokenValue("prueba").header("alg", "RS256").subject("keycloak-7").build();
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CAJERO"))));

        assertThrows(AccessDeniedException.class, () -> service.anular(10L, motivo()));

        verifyNoInteractions(usuarios, ventas, detalles, cajas, stock, devoluciones);
    }

    @Test
    void usuarioSinRolAdminNoPuedeAnular() {
        Jwt jwt = Jwt.withTokenValue("prueba").header("alg", "RS256").subject("keycloak-7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        assertThrows(AccessDeniedException.class, () -> service.anular(10L, motivo()));

        verifyNoInteractions(usuarios, ventas, detalles, cajas, stock, devoluciones);
    }
}
