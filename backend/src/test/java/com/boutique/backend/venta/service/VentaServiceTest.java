package com.boutique.backend.venta.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.boutique.backend.caja.entity.SesionCaja;
import com.boutique.backend.caja.service.CajaVentasService;
import com.boutique.backend.cliente.entity.Cliente;
import com.boutique.backend.cliente.repository.ClienteRepository;
import com.boutique.backend.empresa.entity.Empresa;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.inventario.service.StockService;
import com.boutique.backend.moneda.entity.Moneda;
import com.boutique.backend.moneda.repository.MonedaRepository;
import com.boutique.backend.producto.entity.Producto;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.producto.repository.VarianteProductoRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import com.boutique.backend.venta.dto.DetalleVentaRequest;
import com.boutique.backend.venta.dto.VentaRequest;
import com.boutique.backend.venta.entity.EstadoVenta;
import com.boutique.backend.venta.entity.MetodoPago;
import com.boutique.backend.venta.entity.SerieFactura;
import com.boutique.backend.venta.entity.Venta;
import com.boutique.backend.venta.mapper.DetalleVentaMapper;
import com.boutique.backend.venta.mapper.VentaMapper;
import com.boutique.backend.venta.repository.DetalleVentaRepository;
import com.boutique.backend.venta.repository.SerieFacturaRepository;
import com.boutique.backend.venta.repository.VentaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {
    @Mock VentaRepository ventas;
    @Mock DetalleVentaRepository detalles;
    @Mock UsuarioRepository usuarios;
    @Mock SucursalRepository sucursales;
    @Mock ClienteRepository clientes;
    @Mock MonedaRepository monedas;
    @Mock VarianteProductoRepository variantes;
    @Mock SerieFacturaRepository series;
    @Mock NumeracionFacturaService numeracion;
    @Mock CajaVentasService cajas;
    @Mock StockService stock;
    private VentaService service;
    private Usuario usuario;
    private Sucursal sucursal;
    private SesionCaja caja;
    private Moneda moneda;
    private VarianteProducto variante;
    private SerieFactura serie;

    @BeforeEach
    void preparar() {
        service = new VentaService(ventas, detalles, usuarios, sucursales, clientes, monedas, variantes, series,
            numeracion, cajas, stock, new CalculadoraVenta(), new VentaMapper(), new DetalleVentaMapper());
        sucursal = new Sucursal();
        sucursal.setId(2L);
        sucursal.setEmpresa(new Empresa());
        usuario = new Usuario();
        usuario.setId(7L);
        usuario.setSucursal(sucursal);
        caja = new SesionCaja();
        caja.setId(5L);
        caja.setSucursal(sucursal);
        caja.setAbiertoPor(usuario);
        moneda = new Moneda();
        moneda.setId(1L);
        moneda.setCodigo("NIO");
        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("100.00"));
        variante = new VarianteProducto();
        variante.setId(4L);
        variante.setProducto(producto);
        serie = new SerieFactura();
        serie.setId(3L);
        serie.setSucursal(sucursal);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar() {
        autenticar("ROLE_CAJERO");
    }

    private void autenticar(String rol) {
        Jwt jwt = Jwt.withTokenValue("prueba").header("alg", "RS256").subject("keycloak-7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(rol))));
        when(usuarios.findByKeycloakId("keycloak-7")).thenReturn(Optional.of(usuario));
    }

    private void cabeceraValida() {
        autenticar();
        when(sucursales.findByIdParaVenta(2L)).thenReturn(Optional.of(sucursal));
        when(cajas.obtenerParaVenta(5L, 7L, 2L)).thenReturn(caja);
        when(monedas.findByIdParaVenta(1L)).thenReturn(Optional.of(moneda));
    }

    private void ventaValida() {
        cabeceraValida();
        when(variantes.findAllById(any())).thenReturn(List.of(variante));
    }

    private void persistenciaValida() {
        when(numeracion.reservar(3L, 2L)).thenReturn(new NumeracionFacturaService.NumeroFactura(8L, "FAC-000008"));
        when(series.getReferenceById(3L)).thenReturn(serie);
        when(ventas.saveAndFlush(any())).thenAnswer(inv -> {
            Venta venta = inv.getArgument(0);
            venta.setId(10L);
            return venta;
        });
        when(detalles.saveAllAndFlush(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private VentaRequest solicitud() {
        return new VentaRequest(5L, null, 1L, 3L, MetodoPago.EFECTIVO, new BigDecimal("1.0000"),
            new BigDecimal("250.00"), List.of(new DetalleVentaRequest(4L, 2, new BigDecimal("20.00"))));
    }

    @Test
    void creaFacturaConPrecioDelCatalogoIvaStockYCaja() {
        ventaValida();
        persistenciaValida();
        var r = service.crear(solicitud());
        assertEquals(10L, r.id());
        assertEquals("FAC-000008", r.numeroFactura());
        assertEquals(8L, r.noFactura());
        assertEquals(7L, r.usuarioId());
        assertEquals(2L, r.sucursalId());
        assertEquals(EstadoVenta.COMPLETADA, r.estado());
        assertEquals(new BigDecimal("207.00"), r.total());
        assertEquals(new BigDecimal("100.00"), r.detalles().getFirst().precioUnitario());
        assertEquals(new BigDecimal("180.00"), r.detalles().getFirst().monto());
        assertNull(r.clienteId());
        verify(stock).retirar(4L, 2L, 2);
        verify(cajas).registrarVenta(caja, new BigDecimal("207.00"), MetodoPago.EFECTIVO);
    }

    @Test
    void acumulaCajaEnNioCuandoFacturaEnUsd() {
        ventaValida();
        persistenciaValida();
        moneda.setCodigo("USD");
        variante.getProducto().setPrecio(new BigDecimal("3650.00"));
        var request = new VentaRequest(5L, null, 1L, 3L, MetodoPago.EFECTIVO, new BigDecimal("36.5000"),
            new BigDecimal("120.00"), List.of(new DetalleVentaRequest(4L, 1, null)));
        var r = service.crear(request);
        assertEquals(new BigDecimal("115.00"), r.total());
        assertEquals(new BigDecimal("5.00"), r.cambio());
        verify(cajas).registrarVenta(caja, new BigDecimal("4197.50"), MetodoPago.EFECTIVO);
    }

    @Test
    void agregaVariantesRepetidasYRestaStockEnOrdenEstable() {
        ventaValida();
        persistenciaValida();
        VarianteProducto otra = new VarianteProducto();
        otra.setId(9L);
        otra.setProducto(variante.getProducto());
        when(variantes.findAllById(any())).thenReturn(List.of(otra, variante));
        var request = new VentaRequest(5L, null, 1L, 3L, MetodoPago.TARJETA, new BigDecimal("1.0000"), null,
            List.of(new DetalleVentaRequest(9L, 1, null), new DetalleVentaRequest(4L, 1, null),
                new DetalleVentaRequest(9L, 1, null)));
        assertEquals(3, service.crear(request).detalles().size());
        var orden = inOrder(stock);
        orden.verify(stock).retirar(4L, 2L, 1);
        orden.verify(stock).retirar(9L, 2L, 2);
    }

    @Test
    void exigeUsuarioAutenticado() {
        assertThrows(AccessDeniedException.class, () -> service.crear(solicitud()));
        verifyNoInteractions(ventas, stock, cajas, numeracion);
    }

    @Test
    void exigeUsuarioActivo() {
        autenticar();
        usuario.setActivo(false);
        assertThrows(AccessDeniedException.class, () -> service.crear(solicitud()));
        verifyNoInteractions(ventas, stock, cajas, numeracion);
    }

    @Test
    void exigeSucursalActiva() {
        autenticar();
        sucursal.setActiva(false);
        when(sucursales.findByIdParaVenta(2L)).thenReturn(Optional.of(sucursal));
        assertThrows(ConflictoException.class, () -> service.crear(solicitud()));
        verifyNoInteractions(ventas, stock, cajas, numeracion);
    }

    @Test
    void exigeMonedaActiva() {
        cabeceraValida();
        moneda.setActivo(false);
        assertThrows(ConflictoException.class, () -> service.crear(solicitud()));
        verifyNoInteractions(ventas, stock, numeracion);
    }

    @Test
    void exigeTipoCambioUnoParaNio() {
        cabeceraValida();
        var request = new VentaRequest(5L, null, 1L, 3L, MetodoPago.EFECTIVO, new BigDecimal("36.5000"),
            new BigDecimal("250.00"), solicitud().detalles());
        assertThrows(ConflictoException.class, () -> service.crear(request));
        verifyNoInteractions(ventas, stock, numeracion);
    }

    @Test
    void rechazaClienteInactivo() {
        cabeceraValida();
        Cliente cliente = new Cliente();
        cliente.setActivo(false);
        when(clientes.findById(6L)).thenReturn(Optional.of(cliente));
        var request = new VentaRequest(5L, 6L, 1L, 3L, MetodoPago.EFECTIVO, new BigDecimal("1.0000"),
            new BigDecimal("250.00"), solicitud().detalles());
        assertThrows(ConflictoException.class, () -> service.crear(request));
        verifyNoInteractions(ventas, stock, numeracion);
    }

    @Test
    void rechazaVarianteInexistenteAntesDeEscribir() {
        ventaValida();
        when(variantes.findAllById(any())).thenReturn(List.of());
        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(solicitud()));
        verifyNoInteractions(ventas, stock, numeracion);
    }

    @Test
    void rechazaProductoInactivo() {
        ventaValida();
        variante.getProducto().setActivo(false);
        assertThrows(ConflictoException.class, () -> service.crear(solicitud()));
        verifyNoInteractions(ventas, stock, numeracion);
    }

    @Test
    void faltaDeStockImpideReservarNumeroYGuardarVenta() {
        ventaValida();
        doThrow(new ConflictoException("Stock insuficiente")).when(stock).retirar(4L, 2L, 2);
        assertThrows(ConflictoException.class, () -> service.crear(solicitud()));
        verifyNoInteractions(ventas, detalles, numeracion);
        verify(cajas, never()).registrarVenta(any(), any(), any());
    }

    @Test
    void pagoInsuficienteSeRechazaAntesDeTocarStockYCaja() {
        ventaValida();
        var request = new VentaRequest(5L, null, 1L, 3L, MetodoPago.EFECTIVO, new BigDecimal("1.0000"),
            new BigDecimal("206.99"), solicitud().detalles());
        assertThrows(ConflictoException.class, () -> service.crear(request));
        verifyNoInteractions(ventas, stock, numeracion);
        verify(cajas, never()).registrarVenta(any(), any(), any());
    }

    @Test
    void rechazaDesbordamientoDeCantidadDeLaMismaVariante() {
        cabeceraValida();
        var request = new VentaRequest(5L, null, 1L, 3L, MetodoPago.TARJETA, new BigDecimal("1.0000"), null,
            List.of(new DetalleVentaRequest(4L, Integer.MAX_VALUE, null), new DetalleVentaRequest(4L, 1, null)));
        assertThrows(ConflictoException.class, () -> service.crear(request));
        verifyNoInteractions(ventas, stock, numeracion, variantes);
    }

    @Test
    void traduceConflictoDeFacturaAlGuardarSinOcultarElFallo() {
        ventaValida();
        when(numeracion.reservar(3L, 2L)).thenReturn(new NumeracionFacturaService.NumeroFactura(8L, "FAC-000008"));
        when(series.getReferenceById(3L)).thenReturn(serie);
        when(ventas.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("factura duplicada"));
        assertThrows(ConflictoException.class, () -> service.crear(solicitud()));
        verifyNoInteractions(detalles);
    }

    private Venta ventaGuardada() {
        Venta venta = new Venta();
        venta.setId(10L);
        venta.setSucursal(sucursal);
        venta.setUsuario(usuario);
        venta.setMoneda(moneda);
        venta.setSesionCaja(caja);
        venta.setSerieFactura(serie);
        venta.setNumeroFactura("FAC-000008");
        venta.setNoFactura(8L);
        venta.setTotal(new BigDecimal("207.00"));
        return venta;
    }

    @Test
    void cajeroListaSoloSusVentasAunqueNoEnvieFiltros() {
        autenticar();
        var pageable = PageRequest.of(0, 20);
        when(ventas.buscar(2L, 7L, null, null, pageable))
            .thenReturn(new PageImpl<>(List.of(ventaGuardada()), pageable, 1));
        var r = service.listar(null, null, null, pageable);
        assertEquals(1, r.getTotalElements());
        assertEquals(7L, r.getContent().getFirst().usuarioId());
        verify(ventas).buscar(2L, 7L, null, null, pageable);
    }

    @Test
    void filtroDeCajaNoPermiteAlCajeroQuitarSuRestriccionDeUsuario() {
        autenticar();
        var pageable = PageRequest.of(0, 20);
        when(ventas.buscar(2L, 7L, 99L, EstadoVenta.COMPLETADA, pageable))
            .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        assertTrue(service.listar(null, 99L, EstadoVenta.COMPLETADA, pageable).isEmpty());
        verify(ventas).buscar(2L, 7L, 99L, EstadoVenta.COMPLETADA, pageable);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_CAJERO"})
    void noPermitePedirOtraSucursalEnElListado(String rol) {
        autenticar(rol);
        assertThrows(AccessDeniedException.class, () -> service.listar(9L, null, null, PageRequest.of(0, 20)));
        verifyNoInteractions(ventas, detalles);
    }

    @Test
    void adminListaVentasDeTodosLosCajerosDeSuSucursal() {
        autenticar("ROLE_ADMIN");
        Venta otra = ventaGuardada();
        Usuario cajero = new Usuario();
        cajero.setId(9L);
        otra.setUsuario(cajero);
        var pageable = PageRequest.of(0, 20);
        when(ventas.buscar(2L, null, null, null, pageable))
            .thenReturn(new PageImpl<>(List.of(otra), pageable, 1));
        assertEquals(9L, service.listar(null, null, null, pageable).getContent().getFirst().usuarioId());
        verify(ventas).buscar(2L, null, null, null, pageable);
    }

    @Test
    void cajeroConsultaSuVentaPorId() {
        autenticar();
        when(ventas.findVisibleById(10L, 2L, 7L)).thenReturn(Optional.of(ventaGuardada()));
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of());
        assertEquals(7L, service.obtenerPorId(10L).usuarioId());
        verify(ventas, never()).findById(any());
    }

    @Test
    void cajeroNoAccedeAVentaAjenaCambiandoElId() {
        autenticar();
        when(ventas.findVisibleById(99L, 2L, 7L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
        verifyNoInteractions(detalles);
        verify(ventas, never()).findById(any());
    }

    @Test
    void adminConsultaVentaDeUnCajeroDeSuSucursalPorId() {
        autenticar("ROLE_ADMIN");
        Venta otra = ventaGuardada();
        Usuario cajero = new Usuario();
        cajero.setId(9L);
        otra.setUsuario(cajero);
        when(ventas.findVisibleById(10L, 2L, null)).thenReturn(Optional.of(otra));
        when(detalles.findByVentaIdOrderByIdAsc(10L)).thenReturn(List.of());
        assertEquals(9L, service.obtenerPorId(10L).usuarioId());
        verify(ventas, never()).findById(any());
    }

    @Test
    void adminNoConsultaVentaDeOtraSucursalPorId() {
        autenticar("ROLE_ADMIN");
        when(ventas.findVisibleById(99L, 2L, null)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
        verifyNoInteractions(detalles);
        verify(ventas, never()).findById(any());
    }

    @Test
    void consultasExigenUsuarioAutenticado() {
        assertThrows(AccessDeniedException.class, () -> service.listar(null, null, null, PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> service.obtenerPorId(10L));
        verifyNoInteractions(usuarios, ventas, detalles);
    }

    @Test
    void otroRolNoPuedeConsultarVentasDesdeElServicio() {
        autenticar("ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> service.listar(null, null, null, PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> service.obtenerPorId(10L));
        verifyNoInteractions(ventas, detalles);
    }

    @Test
    void noConsultaVentasConSucursalInactiva() {
        autenticar();
        sucursal.setActiva(false);
        assertThrows(AccessDeniedException.class, () -> service.obtenerPorId(10L));
        verifyNoInteractions(ventas, detalles);
    }
}
