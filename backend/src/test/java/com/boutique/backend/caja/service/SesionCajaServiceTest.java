package com.boutique.backend.caja.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.boutique.backend.caja.dto.SesionCajaAperturaRequest;
import com.boutique.backend.caja.dto.SesionCajaCierreRequest;
import com.boutique.backend.caja.entity.EstadoCaja;
import com.boutique.backend.caja.entity.SesionCaja;
import com.boutique.backend.caja.mapper.SesionCajaMapper;
import com.boutique.backend.caja.repository.SesionCajaRepository;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SesionCajaServiceTest {

    @Mock SesionCajaRepository cajas;
    @Mock UsuarioRepository usuarios;
    @Mock SucursalRepository sucursales;
    private SesionCajaService service;
    private Usuario usuario;
    private Sucursal sucursal;

    @BeforeEach
    void preparar() {
        service = new SesionCajaService(cajas, usuarios, sucursales, new SesionCajaMapper());
        sucursal = new Sucursal();
        sucursal.setId(2L);
        usuario = new Usuario();
        usuario.setId(7L);
        usuario.setSucursal(sucursal);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar() {
        Jwt jwt = Jwt.withTokenValue("prueba").header("alg", "RS256").subject("keycloak-7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        when(usuarios.findByKeycloakId("keycloak-7")).thenReturn(Optional.of(usuario));
    }

    private void permitirApertura() {
        autenticar();
        when(sucursales.findByIdForUpdate(2L)).thenReturn(Optional.of(sucursal));
    }

    private SesionCaja caja() {
        SesionCaja caja = new SesionCaja();
        caja.setId(10L);
        caja.setAbiertoPor(usuario);
        caja.setSucursal(sucursal);
        caja.setEfectivoInicial(new BigDecimal("100.00"));
        return caja;
    }

    private SesionCaja prepararCierre() {
        autenticar();
        SesionCaja caja = caja();
        when(cajas.findByIdForUpdate(10L)).thenReturn(Optional.of(caja));
        return caja;
    }

    @Test
    void abreParaElUsuarioAutenticadoConAcumuladosEnCero() {
        permitirApertura();
        when(cajas.saveAndFlush(any())).thenAnswer(inv -> {
            SesionCaja caja = inv.getArgument(0);
            caja.setId(10L);
            return caja;
        });
        var respuesta = service.abrir(new SesionCajaAperturaRequest(new BigDecimal("50"), " Fondo inicial "));
        assertEquals(7L, respuesta.abiertoPorId());
        assertEquals(2L, respuesta.sucursalId());
        assertEquals(EstadoCaja.ABIERTA, respuesta.estado());
        assertEquals(new BigDecimal("50.00"), respuesta.efectivoInicial());
        assertEquals(new BigDecimal("0.00"), respuesta.totalVentas());
        assertEquals(0, respuesta.cantidadVentas());
        assertEquals("Fondo inicial", respuesta.notas());
        assertNotNull(respuesta.abiertoEn());
        assertNull(respuesta.cerradoEn());
        assertNull(respuesta.cerradoPorId());
        assertNull(respuesta.efectivoCierre());
        assertNull(respuesta.efectivoEsperado());
        assertNull(respuesta.diferencia());
    }

    @Test
    void noAbreUnaSegundaCajaDelMismoUsuario() {
        permitirApertura();
        when(cajas.existsByAbiertoPorIdAndEstado(7L, EstadoCaja.ABIERTA)).thenReturn(true);
        assertThrows(ConflictoException.class,
            () -> service.abrir(new SesionCajaAperturaRequest(BigDecimal.ZERO, null)));
        verify(cajas, never()).saveAndFlush(any());
    }

    @Test
    void traduceDuplicadoConcurrenteAConflicto() {
        permitirApertura();
        when(cajas.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_caja_abierta_usuario"));
        assertThrows(ConflictoException.class,
            () -> service.abrir(new SesionCajaAperturaRequest(BigDecimal.ZERO, null)));
    }

    @Test
    void exigeUsuarioAutenticadoParaAbrirYCerrar() {
        assertThrows(AccessDeniedException.class,
            () -> service.abrir(new SesionCajaAperturaRequest(BigDecimal.ZERO, null)));
        assertThrows(AccessDeniedException.class,
            () -> service.cerrar(10L, new SesionCajaCierreRequest(BigDecimal.ZERO, null)));
        verifyNoInteractions(cajas, usuarios, sucursales);
    }

    @Test
    void exigeUsuarioLocal() {
        autenticar();
        when(usuarios.findByKeycloakId("keycloak-7")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class,
            () -> service.abrir(new SesionCajaAperturaRequest(BigDecimal.ZERO, null)));
        verifyNoInteractions(cajas, sucursales);
    }

    @Test
    void rechazaUsuarioInactivo() {
        autenticar();
        usuario.setActivo(false);
        assertThrows(AccessDeniedException.class,
            () -> service.abrir(new SesionCajaAperturaRequest(BigDecimal.ZERO, null)));
        verifyNoInteractions(cajas, sucursales);
    }

    @Test
    void rechazaUsuarioSinSucursal() {
        autenticar();
        usuario.setSucursal(null);
        assertThrows(AccessDeniedException.class,
            () -> service.abrir(new SesionCajaAperturaRequest(BigDecimal.ZERO, null)));
        verifyNoInteractions(cajas, sucursales);
    }

    @Test
    void rechazaAperturaEnSucursalInactiva() {
        permitirApertura();
        sucursal.setActiva(false);
        assertThrows(ConflictoException.class,
            () -> service.abrir(new SesionCajaAperturaRequest(BigDecimal.ZERO, null)));
        verifyNoInteractions(cajas);
    }

    @Test
    void consultaLaCajaActualDelUsuario() {
        autenticar();
        when(cajas.findByAbiertoPorIdAndEstado(7L, EstadoCaja.ABIERTA)).thenReturn(Optional.of(caja()));
        assertEquals(10L, service.obtenerActual().id());
    }

    @Test
    void indicaQueNoHayCajaActual() {
        autenticar();
        when(cajas.findByAbiertoPorIdAndEstado(7L, EstadoCaja.ABIERTA)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, service::obtenerActual);
    }

    @ParameterizedTest
    @MethodSource("arqueos")
    void calculaFaltanteSobranteOArqueoExactoSinSumarPagosElectronicos(String contado, String diferencia) {
        SesionCaja caja = prepararCierre();
        caja.setTotalEfectivo(new BigDecimal("200.10"));
        caja.setTotalTarjeta(new BigDecimal("900.00"));
        caja.setTotalTransferencia(new BigDecimal("80.00"));
        caja.setTotalVentas(new BigDecimal("1180.10"));
        caja.setCantidadVentas(6);
        when(cajas.save(caja)).thenReturn(caja);

        var respuesta = service.cerrar(10L, new SesionCajaCierreRequest(new BigDecimal(contado), null));

        assertEquals(new BigDecimal("300.10"), respuesta.efectivoEsperado());
        assertEquals(new BigDecimal(diferencia), respuesta.diferencia());
        assertEquals(EstadoCaja.CERRADA, respuesta.estado());
        assertEquals(7L, respuesta.cerradoPorId());
        assertNotNull(respuesta.cerradoEn());
        assertFalse(respuesta.cerradoEn().isBefore(respuesta.abiertoEn()));
        assertEquals(new BigDecimal("1180.10"), respuesta.totalVentas());
        assertEquals(6, respuesta.cantidadVentas());
    }

    static Stream<Arguments> arqueos() {
        return Stream.of(Arguments.of("290.00", "-10.10"), Arguments.of("305.35", "5.25"),
            Arguments.of("300.10", "0.00"));
    }

    @Test
    void permiteCerrarUnaCajaSinVentas() {
        SesionCaja caja = prepararCierre();
        when(cajas.save(caja)).thenReturn(caja);
        var respuesta = service.cerrar(10L, new SesionCajaCierreRequest(new BigDecimal("100.00"), null));
        assertEquals(new BigDecimal("100.00"), respuesta.efectivoEsperado());
        assertEquals(new BigDecimal("0.00"), respuesta.diferencia());
    }

    @Test
    void conservaNotasDeAperturaAlAgregarLasDeCierre() {
        SesionCaja caja = prepararCierre();
        caja.setNotas("Fondo verificado");
        when(cajas.save(caja)).thenReturn(caja);
        var respuesta = service.cerrar(10L, new SesionCajaCierreRequest(BigDecimal.ZERO, " Falta efectivo "));
        assertEquals("Fondo verificado\nCierre: Falta efectivo", respuesta.notas());
    }

    @Test
    void noPermiteCambiarUnArqueoCerrado() {
        SesionCaja caja = prepararCierre();
        caja.setEstado(EstadoCaja.CERRADA);
        caja.setEfectivoCierre(new BigDecimal("100.00"));
        assertThrows(ConflictoException.class,
            () -> service.cerrar(10L, new SesionCajaCierreRequest(new BigDecimal("500.00"), null)));
        assertEquals(new BigDecimal("100.00"), caja.getEfectivoCierre());
        verify(cajas, never()).save(any());
    }

    @Test
    void noPermiteCerrarCajaDeOtroUsuario() {
        SesionCaja caja = prepararCierre();
        Usuario otro = new Usuario();
        otro.setId(8L);
        caja.setAbiertoPor(otro);
        assertThrows(AccessDeniedException.class,
            () -> service.cerrar(10L, new SesionCajaCierreRequest(BigDecimal.ZERO, null)));
        assertEquals(EstadoCaja.ABIERTA, caja.getEstado());
        verify(cajas, never()).save(any());
    }

    @Test
    void noPermiteCerrarCajaDeOtraSucursal() {
        SesionCaja caja = prepararCierre();
        Sucursal otra = new Sucursal();
        otra.setId(3L);
        caja.setSucursal(otra);
        assertThrows(AccessDeniedException.class,
            () -> service.cerrar(10L, new SesionCajaCierreRequest(BigDecimal.ZERO, null)));
        verify(cajas, never()).save(any());
    }

    @Test
    void rechazaDesbordamientoDelEfectivoEsperadoAntesDeCerrar() {
        SesionCaja caja = prepararCierre();
        caja.setEfectivoInicial(new BigDecimal("999999999999.99"));
        caja.setTotalEfectivo(new BigDecimal("0.01"));
        assertThrows(ConflictoException.class,
            () -> service.cerrar(10L, new SesionCajaCierreRequest(BigDecimal.ZERO, null)));
        assertEquals(EstadoCaja.ABIERTA, caja.getEstado());
        assertNull(caja.getCerradoEn());
        verify(cajas, never()).save(any());
    }

    @Test
    void permiteElMaximoExactoDelEfectivoEsperado() {
        SesionCaja caja = prepararCierre();
        caja.setEfectivoInicial(new BigDecimal("999999999999.98"));
        caja.setTotalEfectivo(new BigDecimal("0.01"));
        when(cajas.save(caja)).thenReturn(caja);
        var respuesta = service.cerrar(10L, new SesionCajaCierreRequest(BigDecimal.ZERO, null));
        assertEquals(new BigDecimal("999999999999.99"), respuesta.efectivoEsperado());
        assertEquals(new BigDecimal("-999999999999.99"), respuesta.diferencia());
    }

    @Test
    void informaCajaInexistenteAlCerrar() {
        autenticar();
        when(cajas.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class,
            () -> service.cerrar(99L, new SesionCajaCierreRequest(BigDecimal.ZERO, null)));
        verify(cajas, never()).save(any());
    }
}
