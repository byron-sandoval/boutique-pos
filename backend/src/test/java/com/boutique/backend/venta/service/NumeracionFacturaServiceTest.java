package com.boutique.backend.venta.service;

import java.util.Optional;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.venta.entity.SerieFactura;
import com.boutique.backend.venta.repository.SerieFacturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumeracionFacturaServiceTest {
    @Mock SerieFacturaRepository repository;
    private NumeracionFacturaService service;
    private SerieFactura serie;

    @BeforeEach
    void preparar() {
        service = new NumeracionFacturaService(repository);
        Sucursal sucursal = new Sucursal();
        sucursal.setId(2L);
        serie = new SerieFactura();
        serie.setId(1L);
        serie.setSucursal(sucursal);
    }

    private void encontrada() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(serie));
    }

    @Test
    void reservaNumeroConCerosYAvanzaContador() {
        encontrada();
        var numero = service.reservar(1L, 2L);
        assertEquals(1L, numero.numero());
        assertEquals("FAC-000001", numero.numeroFactura());
        assertEquals(2L, serie.getSiguienteNumero());
        verify(repository).save(serie);
    }

    @Test
    void llamadasSucesivasAvanzanSinReiniciarLaSerie() {
        encontrada();
        assertEquals("FAC-000001", service.reservar(1L, 2L).numeroFactura());
        assertEquals("FAC-000002", service.reservar(1L, 2L).numeroFactura());
        assertEquals(3L, serie.getSiguienteNumero());
    }

    @Test
    void digitosEsRellenoMinimoYNuncaTruncaElNumero() {
        encontrada();
        serie.setDigitos(2);
        serie.setSiguienteNumero(100L);
        assertEquals("FAC-100", service.reservar(1L, 2L).numeroFactura());
    }

    @Test
    void noReservaNumerosDeOtraSucursal() {
        encontrada();
        assertThrows(ConflictoException.class, () -> service.reservar(1L, 3L));
        assertEquals(1L, serie.getSiguienteNumero());
        verify(repository, never()).save(any());
    }

    @Test
    void noReservaSiLaSerieEstaInactiva() {
        encontrada();
        serie.setActiva(false);
        assertThrows(ConflictoException.class, () -> service.reservar(1L, 2L));
        assertEquals(1L, serie.getSiguienteNumero());
        verify(repository, never()).save(any());
    }

    @Test
    void noReservaSiLaSucursalEstaInactiva() {
        encontrada();
        serie.getSucursal().setActiva(false);
        assertThrows(ConflictoException.class, () -> service.reservar(1L, 2L));
        verify(repository, never()).save(any());
    }

    @Test
    void evitaDesbordamientoDelContador() {
        encontrada();
        serie.setSiguienteNumero(Long.MAX_VALUE);
        assertThrows(ConflictoException.class, () -> service.reservar(1L, 2L));
        assertEquals(Long.MAX_VALUE, serie.getSiguienteNumero());
        verify(repository, never()).save(any());
    }

    @Test
    void alcanzaElUltimoContadorRepresentableSinPasarANegativo() {
        encontrada();
        serie.setSiguienteNumero(Long.MAX_VALUE - 1);
        assertEquals(Long.MAX_VALUE - 1, service.reservar(1L, 2L).numero());
        assertEquals(Long.MAX_VALUE, serie.getSiguienteNumero());
        assertThrows(ConflictoException.class, () -> service.reservar(1L, 2L));
    }

    @Test
    void informaSerieInexistente() {
        when(repository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.reservar(99L, 2L));
        verify(repository, never()).save(any());
    }
}
