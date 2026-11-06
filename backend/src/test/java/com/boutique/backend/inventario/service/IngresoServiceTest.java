package com.boutique.backend.inventario.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.inventario.dto.DetalleIngresoRequest;
import com.boutique.backend.inventario.dto.IngresoRequest;
import com.boutique.backend.inventario.entity.DetalleIngreso;
import com.boutique.backend.inventario.entity.Ingreso;
import com.boutique.backend.inventario.mapper.DetalleIngresoMapper;
import com.boutique.backend.inventario.mapper.IngresoMapper;
import com.boutique.backend.inventario.repository.DetalleIngresoRepository;
import com.boutique.backend.inventario.repository.IngresoRepository;
import com.boutique.backend.producto.entity.Producto;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.producto.repository.VarianteProductoRepository;
import com.boutique.backend.proveedor.entity.Proveedor;
import com.boutique.backend.proveedor.repository.ProveedorRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngresoServiceTest {

    @Mock IngresoRepository ingresos;
    @Mock DetalleIngresoRepository detalles;
    @Mock SucursalRepository sucursales;
    @Mock VarianteProductoRepository variantes;
    @Mock ProveedorRepository proveedores;
    @Mock UsuarioRepository usuarios;
    @Mock StockService stock;
    private IngresoService service;
    private Sucursal sucursal;
    private Usuario usuario;
    private VarianteProducto variante;

    @BeforeEach
    void preparar() {
        service = new IngresoService(ingresos, detalles, sucursales, variantes, proveedores, usuarios,
            new IngresoMapper(), new DetalleIngresoMapper(), stock);
        sucursal = new Sucursal();
        sucursal.setId(1L);
        usuario = new Usuario();
        usuario.setId(8L);
        usuario.setSucursal(sucursal);
        variante = new VarianteProducto();
        variante.setId(4L);
        variante.setProducto(new Producto());
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar() {
        Jwt jwt = Jwt.withTokenValue("prueba").header("alg", "RS256").subject("keycloak-8").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        when(usuarios.findByKeycloakId("keycloak-8")).thenReturn(Optional.of(usuario));
    }

    private void sucursalActiva() {
        autenticar();
        when(sucursales.findByIdForUpdate(1L)).thenReturn(Optional.of(sucursal));
    }

    private IngresoRequest solicitud(DetalleIngresoRequest... lineas) {
        return new IngresoRequest(1L, null, " FAC-1 ", null, List.of(lineas));
    }

    private DetalleIngresoRequest linea(int cantidad, String costo) {
        return new DetalleIngresoRequest(4L, cantidad, new BigDecimal(costo));
    }

    private Ingreso ingreso() {
        Ingreso ingreso = new Ingreso();
        ingreso.setId(10L);
        ingreso.setSucursal(sucursal);
        ingreso.setUsuario(usuario);
        return ingreso;
    }

    private DetalleIngreso detalle(Ingreso ingreso, int cantidad) {
        DetalleIngreso detalle = new DetalleIngreso();
        detalle.setIngreso(ingreso);
        detalle.setVariante(variante);
        detalle.setCantidad(cantidad);
        return detalle;
    }

    @Test
    void calculaImportesConDecimalesYAcumulaVariantesRepetidas() {
        sucursalActiva();
        when(variantes.findAllById(any())).thenReturn(List.of(variante));
        when(ingresos.save(any())).thenAnswer(inv -> {
            Ingreso ingreso = inv.getArgument(0);
            ingreso.setId(10L);
            return ingreso;
        });
        when(detalles.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.crear(solicitud(linea(3, "0.10"), linea(2, "2.35")));

        assertEquals(new BigDecimal("5.00"), response.total());
        assertEquals(new BigDecimal("0.30"), response.detalles().getFirst().monto());
        assertEquals(8L, response.usuarioId());
        assertEquals("FAC-1", response.noDocumento());
        assertNull(response.proveedorId());
        verify(stock).ingresar(4L, 1L, 5);
        verify(ingresos).existsBySucursalIdAndProveedorIdAndNoDocumentoAndActivoTrue(1L, null, "FAC-1");
    }

    @Test
    void exigeUsuarioAutenticadoAntesDeEscribir() {
        assertThrows(AccessDeniedException.class, () -> service.crear(solicitud(linea(1, "1.00"))));
        verifyNoInteractions(ingresos, detalles, stock);
    }

    @Test
    void exigeUsuarioRegistrado() {
        autenticar();
        when(usuarios.findByKeycloakId("keycloak-8")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.crear(solicitud(linea(1, "1.00"))));
        verifyNoInteractions(ingresos, stock);
    }

    @Test
    void rechazaUsuarioInactivo() {
        autenticar();
        usuario.setActivo(false);
        assertThrows(AccessDeniedException.class, () -> service.crear(solicitud(linea(1, "1.00"))));
        verifyNoInteractions(ingresos, stock);
    }

    @Test
    void noPermiteCrearEnOtraSucursal() {
        autenticar();
        var request = new IngresoRequest(2L, null, "A", null, List.of(linea(1, "1.00")));
        assertThrows(AccessDeniedException.class, () -> service.crear(request));
        verifyNoInteractions(ingresos, sucursales, stock);
    }

    @Test
    void rechazaSucursalInactiva() {
        sucursalActiva();
        sucursal.setActiva(false);
        assertThrows(ConflictoException.class, () -> service.crear(solicitud(linea(1, "1.00"))));
        verifyNoInteractions(ingresos, stock);
    }

    @Test
    void rechazaDocumentoDuplicadoAntesDeGuardar() {
        sucursalActiva();
        when(ingresos.existsBySucursalIdAndProveedorIdAndNoDocumentoAndActivoTrue(1L, null, "FAC-1"))
            .thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.crear(solicitud(linea(1, "1.00"))));
        verify(ingresos, never()).save(any());
        verifyNoInteractions(stock, detalles, variantes);
    }

    @Test
    void rechazaProveedorInactivo() {
        sucursalActiva();
        Proveedor proveedor = new Proveedor();
        proveedor.setActivo(false);
        when(proveedores.findById(6L)).thenReturn(Optional.of(proveedor));
        var request = new IngresoRequest(1L, 6L, "A", null, List.of(linea(1, "1.00")));
        assertThrows(ConflictoException.class, () -> service.crear(request));
        verifyNoInteractions(ingresos, stock);
    }

    @Test
    void rechazaVarianteInexistente() {
        sucursalActiva();
        when(variantes.findAllById(any())).thenReturn(List.of());
        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(solicitud(linea(1, "1.00"))));
        verify(ingresos, never()).save(any());
        verifyNoInteractions(stock);
    }

    @Test
    void rechazaProductoInactivo() {
        sucursalActiva();
        variante.getProducto().setActivo(false);
        when(variantes.findAllById(any())).thenReturn(List.of(variante));
        assertThrows(ConflictoException.class, () -> service.crear(solicitud(linea(1, "1.00"))));
        verify(ingresos, never()).save(any());
        verifyNoInteractions(stock);
    }

    @Test
    void rechazaDesbordamientoDelMontoDeUnaLinea() {
        sucursalActiva();
        assertThrows(ConflictoException.class, () -> service.crear(solicitud(linea(2, "9999999999.99"))));
        verify(ingresos, never()).save(any());
        verifyNoInteractions(stock, detalles);
    }

    @Test
    void rechazaDesbordamientoDelTotalAunqueLasLineasSeanValidas() {
        sucursalActiva();
        assertThrows(ConflictoException.class, () -> service.crear(
            solicitud(linea(1, "6000000000.00"), linea(1, "6000000000.00"))));
        verify(ingresos, never()).save(any());
        verifyNoInteractions(stock, detalles);
    }

    @Test
    void rechazaDesbordamientoDeCantidadesRepetidas() {
        sucursalActiva();
        assertThrows(ConflictoException.class, () -> service.crear(
            solicitud(linea(Integer.MAX_VALUE, "0.00"), linea(1, "0.00"))));
        verify(ingresos, never()).save(any());
        verifyNoInteractions(stock, detalles);
    }

    @Test
    void anulaRevirtiendoStockYConservaDetalles() {
        sucursalActiva();
        Ingreso ingreso = ingreso();
        when(ingresos.findByIdForUpdate(10L)).thenReturn(Optional.of(ingreso));
        when(detalles.findByIngresoIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(ingreso, 3), detalle(ingreso, 2)));

        service.anular(10L);

        verify(stock).retirar(4L, 1L, 5);
        assertFalse(ingreso.getActivo());
        verify(ingresos).save(ingreso);
        verify(detalles, never()).deleteAll(anyList());
    }

    @Test
    void segundaAnulacionNoRestaDeNuevo() {
        sucursalActiva();
        Ingreso ingreso = ingreso();
        ingreso.setActivo(false);
        when(ingresos.findByIdForUpdate(10L)).thenReturn(Optional.of(ingreso));
        service.anular(10L);
        verifyNoInteractions(stock, detalles);
        verify(ingresos, never()).save(any());
    }

    @Test
    void faltaDeStockImpideMarcarIngresoComoAnulado() {
        sucursalActiva();
        Ingreso ingreso = ingreso();
        when(ingresos.findByIdForUpdate(10L)).thenReturn(Optional.of(ingreso));
        when(detalles.findByIngresoIdOrderByIdAsc(10L)).thenReturn(List.of(detalle(ingreso, 5)));
        doThrow(new ConflictoException("Stock insuficiente")).when(stock).retirar(4L, 1L, 5);
        assertThrows(ConflictoException.class, () -> service.anular(10L));
        assertTrue(ingreso.getActivo());
        verify(ingresos, never()).save(any());
    }

    @Test
    void noAnulaIngresoDeOtraSucursal() {
        sucursalActiva();
        Ingreso ingreso = ingreso();
        Sucursal otra = new Sucursal();
        otra.setId(2L);
        ingreso.setSucursal(otra);
        when(ingresos.findByIdForUpdate(10L)).thenReturn(Optional.of(ingreso));
        assertThrows(AccessDeniedException.class, () -> service.anular(10L));
        verifyNoInteractions(stock, detalles);
    }
}
