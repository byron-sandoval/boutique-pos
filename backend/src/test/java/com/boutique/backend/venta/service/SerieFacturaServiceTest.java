package com.boutique.backend.venta.service;

import java.util.Optional;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.venta.dto.SerieFacturaCreateRequest;
import com.boutique.backend.venta.dto.SerieFacturaUpdateRequest;
import com.boutique.backend.venta.entity.SerieFactura;
import com.boutique.backend.venta.mapper.SerieFacturaMapper;
import com.boutique.backend.venta.repository.SerieFacturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SerieFacturaServiceTest {
    @Mock SerieFacturaRepository repository;
    @Mock SucursalRepository sucursales;
    private SerieFacturaService service;
    private Sucursal sucursal;

    @BeforeEach
    void preparar() {
        service = new SerieFacturaService(repository, sucursales, new SerieFacturaMapper());
        sucursal = new Sucursal();
        sucursal.setId(2L);
    }

    private SerieFactura serie() {
        SerieFactura serie = new SerieFactura();
        serie.setId(1L);
        serie.setSucursal(sucursal);
        serie.setSiguienteNumero(80L);
        return serie;
    }

    @Test
    void creaConPrefijoNormalizadoYContadorInicial() {
        when(sucursales.findById(2L)).thenReturn(Optional.of(sucursal));
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var response = service.crear(new SerieFacturaCreateRequest(2L, "fac-", 6, 80L, true));
        assertEquals("FAC-", response.prefijo());
        assertEquals(80L, response.siguienteNumero());
        assertEquals(2L, response.sucursalId());
        verify(repository).existsBySucursalIdAndPrefijo(2L, "FAC-");
    }

    @Test
    void impidePrefijoRepetidoDentroDeUnaSucursal() {
        when(sucursales.findById(2L)).thenReturn(Optional.of(sucursal));
        when(repository.existsBySucursalIdAndPrefijo(2L, "FAC-")).thenReturn(true);
        assertThrows(ConflictoException.class,
            () -> service.crear(new SerieFacturaCreateRequest(2L, "fac-", 6, 1L, true)));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void traduceDuplicadoConcurrenteAConflicto() {
        when(sucursales.findById(2L)).thenReturn(Optional.of(sucursal));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicado"));
        assertThrows(ConflictoException.class,
            () -> service.crear(new SerieFacturaCreateRequest(2L, "FAC-", 6, 1L, true)));
    }

    @Test
    void exigeSucursalExistente() {
        when(sucursales.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class,
            () -> service.crear(new SerieFacturaCreateRequest(99L, "FAC-", 6, 1L, true)));
        verifyNoInteractions(repository);
    }

    @Test
    void exigeSucursalActiva() {
        sucursal.setActiva(false);
        when(sucursales.findById(2L)).thenReturn(Optional.of(sucursal));
        assertThrows(ConflictoException.class,
            () -> service.crear(new SerieFacturaCreateRequest(2L, "FAC-", 6, 1L, true)));
        verifyNoInteractions(repository);
    }

    @Test
    void permiteCorregirFormatoAntesDeRegistrarVentasSinReiniciarContador() {
        SerieFactura serie = serie();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(serie));
        when(repository.saveAndFlush(serie)).thenReturn(serie);
        var response = service.actualizar(1L, new SerieFacturaUpdateRequest("ticket-", 8, true));
        assertEquals("TICKET-", response.prefijo());
        assertEquals(8, response.digitos());
        assertEquals(80L, response.siguienteNumero());
        assertEquals(2L, response.sucursalId());
    }

    @Test
    void noPermiteCambiarPrefijoCuandoYaExistenVentas() {
        SerieFactura serie = serie();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(serie));
        when(repository.contarVentas(1L)).thenReturn(1L);
        assertThrows(ConflictoException.class,
            () -> service.actualizar(1L, new SerieFacturaUpdateRequest("OTRA-", 6, true)));
        assertEquals("FAC-", serie.getPrefijo());
        assertEquals(80L, serie.getSiguienteNumero());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void noPermiteCambiarDigitosCuandoYaExistenVentas() {
        SerieFactura serie = serie();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(serie));
        when(repository.contarVentas(1L)).thenReturn(1L);
        assertThrows(ConflictoException.class,
            () -> service.actualizar(1L, new SerieFacturaUpdateRequest("FAC-", 8, true)));
        assertEquals(6, serie.getDigitos());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void permiteDesactivarUnaSerieUsadaSinCambiarSuFormato() {
        SerieFactura serie = serie();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(serie));
        when(repository.saveAndFlush(serie)).thenReturn(serie);
        assertFalse(service.actualizar(1L, new SerieFacturaUpdateRequest("FAC-", 6, false)).activa());
        verify(repository, never()).contarVentas(any());
    }

    @Test
    void borradoLogicoConservaFormatoYContador() {
        SerieFactura serie = serie();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(serie));
        when(repository.saveAndFlush(serie)).thenReturn(serie);
        var response = service.cambiarEstado(1L, false);
        assertFalse(response.activa());
        assertEquals("FAC-", response.prefijo());
        assertEquals(80L, response.siguienteNumero());
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void consultaNoConsumeLaNumeracion() {
        when(repository.findById(1L)).thenReturn(Optional.of(serie()));
        assertEquals(80L, service.obtenerPorId(1L).siguienteNumero());
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
    }
}
