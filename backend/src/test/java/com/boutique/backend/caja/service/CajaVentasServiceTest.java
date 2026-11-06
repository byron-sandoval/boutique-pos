package com.boutique.backend.caja.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.boutique.backend.caja.entity.EstadoCaja;
import com.boutique.backend.caja.entity.SesionCaja;
import com.boutique.backend.caja.repository.SesionCajaRepository;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.venta.entity.MetodoPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CajaVentasServiceTest {
    @Mock SesionCajaRepository repository;
    private CajaVentasService service;
    private SesionCaja caja;

    @BeforeEach
    void preparar() {
        service = new CajaVentasService(repository);
        caja = new SesionCaja();
        caja.setId(1L);
        Sucursal sucursal = new Sucursal();
        sucursal.setId(2L);
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        caja.setSucursal(sucursal);
        caja.setAbiertoPor(usuario);
    }

    @ParameterizedTest
    @EnumSource(MetodoPago.class)
    void acumulaLaVentaSoloEnSuMetodoDePago(MetodoPago metodoPago) {
        BigDecimal importe = new BigDecimal("115.10");
        service.registrarVenta(caja, importe, metodoPago);
        assertEquals(importe, caja.getTotalVentas());
        assertEquals(1, caja.getCantidadVentas());
        assertEquals(metodoPago == MetodoPago.EFECTIVO ? importe : new BigDecimal("0.00"), caja.getTotalEfectivo());
        assertEquals(metodoPago == MetodoPago.TARJETA ? importe : new BigDecimal("0.00"), caja.getTotalTarjeta());
        assertEquals(metodoPago == MetodoPago.TRANSFERENCIA ? importe : new BigDecimal("0.00"), caja.getTotalTransferencia());
    }

    @Test
    void noPermiteVenderEnUnaCajaCerrada() {
        caja.setEstado(EstadoCaja.CERRADA);
        assertThrows(ConflictoException.class, () -> service.registrarVenta(caja, BigDecimal.TEN, MetodoPago.EFECTIVO));
        verifyNoInteractions(repository);
    }

    @Test
    void exigeCajaPropiaEnLaSucursalDelUsuario() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(caja));
        assertSame(caja, service.obtenerParaVenta(1L, 3L, 2L));
        assertThrows(AccessDeniedException.class, () -> service.obtenerParaVenta(1L, 4L, 2L));
        assertThrows(AccessDeniedException.class, () -> service.obtenerParaVenta(1L, 3L, 5L));
    }

    @Test
    void compruebaLimiteDeCierreAntesDeCambiarAcumulados() {
        caja.setEfectivoInicial(new BigDecimal("999999999999.99"));
        assertThrows(ConflictoException.class,
            () -> service.registrarVenta(caja, new BigDecimal("0.01"), MetodoPago.EFECTIVO));
        assertEquals(new BigDecimal("0.00"), caja.getTotalVentas());
        assertEquals(0, caja.getCantidadVentas());
        verify(repository, never()).save(any());
    }

    @Test
    void compruebaLimiteDelTotalAunqueElPagoNoSeaEfectivo() {
        caja.setTotalVentas(new BigDecimal("999999999999.99"));
        assertThrows(ConflictoException.class,
            () -> service.registrarVenta(caja, new BigDecimal("0.01"), MetodoPago.TARJETA));
        assertEquals(new BigDecimal("0.00"), caja.getTotalTarjeta());
        verify(repository, never()).save(any());
    }

    @Test
    void compruebaDesbordamientoDelContadorAntesDeModificarLaCaja() {
        caja.setCantidadVentas(Integer.MAX_VALUE);
        assertThrows(ConflictoException.class,
            () -> service.registrarVenta(caja, BigDecimal.TEN, MetodoPago.TRANSFERENCIA));
        assertEquals(new BigDecimal("0.00"), caja.getTotalVentas());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(MetodoPago.class)
    void anulaRevirtiendoSoloElMetodoOriginalYConservaFondoInicial(MetodoPago metodoPago) {
        caja.setEfectivoInicial(new BigDecimal("100.00"));
        service.registrarVenta(caja, new BigDecimal("115.10"), metodoPago);
        service.anularVenta(caja, new BigDecimal("115.10"), metodoPago);
        assertEquals(new BigDecimal("0.00"), caja.getTotalVentas());
        assertEquals(new BigDecimal("0.00"), caja.getTotalEfectivo());
        assertEquals(new BigDecimal("0.00"), caja.getTotalTarjeta());
        assertEquals(new BigDecimal("0.00"), caja.getTotalTransferencia());
        assertEquals(new BigDecimal("100.00"), caja.getEfectivoInicial());
        assertEquals(0, caja.getCantidadVentas());
        assertNull(caja.getEfectivoCierre());
    }

    @Test
    void anularEfectivoNoDescuentaTarjetaNiOtrasVentas() {
        caja.setTotalVentas(new BigDecimal("300.00"));
        caja.setTotalEfectivo(new BigDecimal("100.00"));
        caja.setTotalTarjeta(new BigDecimal("200.00"));
        caja.setCantidadVentas(2);
        service.anularVenta(caja, new BigDecimal("100.00"), MetodoPago.EFECTIVO);
        assertEquals(new BigDecimal("200.00"), caja.getTotalVentas());
        assertEquals(new BigDecimal("200.00"), caja.getTotalTarjeta());
        assertEquals(new BigDecimal("0.00"), caja.getTotalEfectivo());
        assertEquals(1, caja.getCantidadVentas());
    }

    @Test
    void noAnulaEnCajaCerrada() {
        caja.setEstado(EstadoCaja.CERRADA);
        assertThrows(ConflictoException.class, () -> service.anularVenta(caja, BigDecimal.TEN, MetodoPago.EFECTIVO));
        verifyNoInteractions(repository);
    }

    @Test
    void noRestaMasDelAcumuladoDelMetodoDePago() {
        caja.setTotalVentas(new BigDecimal("100.00"));
        caja.setTotalTarjeta(new BigDecimal("100.00"));
        caja.setCantidadVentas(1);
        assertThrows(ConflictoException.class, () -> service.anularVenta(caja, BigDecimal.TEN, MetodoPago.EFECTIVO));
        assertEquals(new BigDecimal("100.00"), caja.getTotalVentas());
        assertEquals(1, caja.getCantidadVentas());
        verify(repository, never()).save(any());
    }

    @Test
    void noPermiteContadorDeVentasNegativo() {
        caja.setTotalVentas(new BigDecimal("100.00"));
        caja.setTotalEfectivo(new BigDecimal("100.00"));
        caja.setCantidadVentas(0);
        assertThrows(ConflictoException.class, () -> service.anularVenta(caja, BigDecimal.TEN, MetodoPago.EFECTIVO));
        assertEquals(new BigDecimal("100.00"), caja.getTotalVentas());
        assertEquals(new BigDecimal("100.00"), caja.getTotalEfectivo());
        verify(repository, never()).save(any());
    }

    @Test
    void permiteAnularVentaDeImporteCeroContabilizada() {
        caja.setCantidadVentas(1);
        service.anularVenta(caja, new BigDecimal("0.00"), MetodoPago.EFECTIVO);
        assertEquals(0, caja.getCantidadVentas());
        assertEquals(new BigDecimal("0.00"), caja.getTotalVentas());
    }

    @Test
    void noPermiteQueUnaAnulacionNegativaSumeDinero() {
        assertThrows(ConflictoException.class,
            () -> service.anularVenta(caja, new BigDecimal("-1.00"), MetodoPago.TARJETA));
        verifyNoInteractions(repository);
    }

    @Test
    void cajaParaAnulacionPuedePertenecerAOtroUsuarioDeLaMismaSucursal() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(caja));

        assertSame(caja, service.obtenerParaAnulacion(1L, 2L));
        assertEquals(3L, caja.getAbiertoPor().getId());
    }

    @Test
    void cajaParaAnulacionDebePertenecerALaSucursalDelAdministrador() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(caja));

        assertThrows(AccessDeniedException.class, () -> service.obtenerParaAnulacion(1L, 9L));
        verify(repository, never()).save(any());
    }

    @Test
    void cajaParaAnulacionDebeSeguirAbierta() {
        caja.setEstado(EstadoCaja.CERRADA);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(caja));

        assertThrows(ConflictoException.class, () -> service.obtenerParaAnulacion(1L, 2L));
        verify(repository, never()).save(any());
    }
}
