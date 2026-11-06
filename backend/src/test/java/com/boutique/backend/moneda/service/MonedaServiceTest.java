package com.boutique.backend.moneda.service;

import java.util.List;
import java.util.Optional;

import com.boutique.backend.moneda.dto.MonedaRequest;
import com.boutique.backend.moneda.entity.Moneda;
import com.boutique.backend.moneda.mapper.MonedaMapper;
import com.boutique.backend.moneda.repository.MonedaRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonedaServiceTest {

    @Mock MonedaRepository repository;
    private MonedaService service;

    @BeforeEach
    void preparar() {
        service = new MonedaService(repository, new MonedaMapper());
    }

    private Moneda moneda() {
        Moneda moneda = new Moneda();
        moneda.setId(1L);
        moneda.setCodigo("NIO");
        moneda.setNombre("Córdoba");
        moneda.setSimbolo("C$");
        return moneda;
    }

    @Test
    void normalizaCodigoYTextosAntesDeGuardar() {
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var response = service.crear(new MonedaRequest("nio", " Córdoba ", " C$ ", true));
        assertEquals("NIO", response.codigo());
        assertEquals("Córdoba", response.nombre());
        assertEquals("C$", response.simbolo());
        verify(repository).existsByCodigoIgnoreCase("NIO");
    }

    @Test
    void rechazaCodigoDuplicadoIncluyendoMonedasInactivas() {
        when(repository.existsByCodigoIgnoreCase("NIO")).thenReturn(true);
        assertThrows(ConflictoException.class,
            () -> service.crear(new MonedaRequest("nio", "Córdoba", "C$", false)));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void traduceDuplicadoConcurrenteAConflicto() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("codigo duplicado"));
        assertThrows(ConflictoException.class,
            () -> service.crear(new MonedaRequest("NIO", "Córdoba", "C$", true)));
    }

    @Test
    void actualizaLaMismaMonedaSinConfundirlaConDuplicado() {
        Moneda moneda = moneda();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(moneda));
        when(repository.saveAndFlush(moneda)).thenReturn(moneda);
        var response = service.actualizar(1L, new MonedaRequest("nio", " Córdoba actualizada ", "C$", false));
        assertEquals(1L, response.id());
        assertEquals("Córdoba actualizada", response.nombre());
        assertFalse(response.activo());
        verify(repository).existsByCodigoIgnoreCaseAndIdNot("NIO", 1L);
        verify(repository, never()).contarVentas(any());
    }

    @Test
    void noCambiaCodigoDeMonedaUsadaEnVentas() {
        Moneda moneda = moneda();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(moneda));
        when(repository.contarVentas(1L)).thenReturn(1L);
        assertThrows(ConflictoException.class,
            () -> service.actualizar(1L, new MonedaRequest("EUR", "Euro", "€", true)));
        assertEquals("NIO", moneda.getCodigo());
        assertEquals("Córdoba", moneda.getNombre());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void permiteCorregirCodigoSiNoTieneVentas() {
        Moneda moneda = moneda();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(moneda));
        when(repository.contarVentas(1L)).thenReturn(0L);
        when(repository.saveAndFlush(moneda)).thenReturn(moneda);
        var response = service.actualizar(1L, new MonedaRequest("EUR", "Euro", "€", true));
        assertEquals("EUR", response.codigo());
    }

    @Test
    void noPermiteCambiarAlCodigoDeOtraMoneda() {
        Moneda moneda = moneda();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(moneda));
        when(repository.existsByCodigoIgnoreCaseAndIdNot("USD", 1L)).thenReturn(true);
        assertThrows(ConflictoException.class,
            () -> service.actualizar(1L, new MonedaRequest("USD", "Dólar", "$", true)));
        assertEquals("NIO", moneda.getCodigo());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void desactivaSinBorrarLaMonedaNiCambiarSusDatos() {
        Moneda moneda = moneda();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(moneda));
        when(repository.saveAndFlush(moneda)).thenReturn(moneda);
        assertFalse(service.cambiarEstado(1L, false).activo());
        assertEquals("NIO", moneda.getCodigo());
        assertEquals("Córdoba", moneda.getNombre());
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void permiteReactivarMoneda() {
        Moneda moneda = moneda();
        moneda.setActivo(false);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(moneda));
        when(repository.saveAndFlush(moneda)).thenReturn(moneda);
        assertTrue(service.cambiarEstado(1L, true).activo());
    }

    @Test
    void obtienePorId() {
        when(repository.findById(1L)).thenReturn(Optional.of(moneda()));
        assertEquals("NIO", service.obtenerPorId(1L).codigo());
    }

    @Test
    void informaMonedaInexistenteAlConsultarODesactivar() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        when(repository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarEstado(99L, false));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void listaTodasSinFiltro() {
        var pageable = PageRequest.of(0, 20);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(moneda()), pageable, 1));
        assertEquals("NIO", service.listar(null, pageable).getContent().getFirst().codigo());
        verify(repository, never()).findByActivo(anyBoolean(), any());
    }

    @Test
    void filtraSoloMonedasActivas() {
        var pageable = PageRequest.of(0, 20);
        when(repository.findByActivo(true, pageable)).thenReturn(new PageImpl<>(List.of(moneda()), pageable, 1));
        assertEquals(1, service.listar(true, pageable).getTotalElements());
        verify(repository, never()).findAll(pageable);
    }
}
