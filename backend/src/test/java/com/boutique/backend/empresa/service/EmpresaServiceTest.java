package com.boutique.backend.empresa.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.boutique.backend.empresa.dto.EmpresaRequest;
import com.boutique.backend.empresa.dto.EmpresaResponse;
import com.boutique.backend.empresa.entity.Empresa;
import com.boutique.backend.empresa.mapper.EmpresaMapper;
import com.boutique.backend.empresa.repository.EmpresaRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private LogoStorage logoStorage;

    private EmpresaService service;

    @BeforeEach
    void setUp() {
        service = new EmpresaService(empresaRepository, new EmpresaMapper(), logoStorage);
    }

    @Test
    void rechazaRucDuplicadoAunqueTengaEspacios() {
        when(empresaRepository.existsByRuc("RUC-001")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.crear(request(" RUC-001 ")));

        verify(empresaRepository, never()).saveAndFlush(any(Empresa.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void permiteRucOpcionalSinGuardarCadenasVacias(String ruc) {
        when(empresaRepository.saveAndFlush(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa empresa = invocation.getArgument(0);
            empresa.setId(1L);
            return empresa;
        });

        EmpresaResponse response = service.crear(request(ruc));

        assertEquals(1L, response.id());
        assertNull(response.ruc());
        verify(empresaRepository, never()).existsByRuc(anyString());
    }

    @Test
    void permiteActualizarLaEmpresaConSuMismoRuc() {
        Empresa empresa = new Empresa();
        empresa.setId(4L);
        empresa.setRuc("RUC-001");
        when(empresaRepository.findOneById(4L)).thenReturn(Optional.of(empresa));
        when(empresaRepository.existsByRucAndIdNot("RUC-001", 4L)).thenReturn(false);
        when(empresaRepository.saveAndFlush(empresa)).thenReturn(empresa);

        EmpresaResponse response = service.actualizar(4L, request("RUC-001"));

        assertEquals(4L, response.id());
        assertEquals("RUC-001", response.ruc());
        verify(empresaRepository, never()).existsByRuc(anyString());
    }

    @Test
    void conservaElLogoActualCuandoNoSeAdjuntaOtro() {
        Empresa empresa = new Empresa();
        empresa.setId(4L);
        empresa.setLogoUrl("https://example.com/logo.png");
        when(empresaRepository.findOneById(4L)).thenReturn(Optional.of(empresa));
        when(empresaRepository.saveAndFlush(empresa)).thenReturn(empresa);

        assertEquals("https://example.com/logo.png", service.actualizar(4L, request(null), null).logoUrl());
        verify(logoStorage, never()).subir(any(), any());
    }

    @Test
    void guardaLaUrlQueDevuelveStorageCuandoSeAdjuntaUnLogo() {
        var logo = new MockMultipartFile("logo", "logo.png", "image/png", new byte[] {1});
        String url = "https://example.com/storage/v1/object/public/marca/logos/logo.png";
        when(logoStorage.subir(logo, null)).thenReturn(url);
        when(empresaRepository.saveAndFlush(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(url, service.crear(request(null), logo).logoUrl());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void soloEntregaElLogoAnteriorParaEliminarSiOtraEmpresaNoLoUsa(boolean compartido) {
        var logo = new MockMultipartFile("logo", "logo.png", "image/png", new byte[] {1});
        String anterior = "https://example.com/anterior.png";
        String nuevo = "https://example.com/nuevo.png";
        Empresa empresa = new Empresa();
        empresa.setId(4L);
        empresa.setLogoUrl(anterior);
        when(empresaRepository.findOneById(4L)).thenReturn(Optional.of(empresa));
        when(empresaRepository.existsByLogoUrlAndIdNot(anterior, 4L)).thenReturn(compartido);
        when(logoStorage.subir(logo, compartido ? null : anterior)).thenReturn(nuevo);
        when(empresaRepository.saveAndFlush(empresa)).thenReturn(empresa);

        assertEquals(nuevo, service.actualizar(4L, request(null), logo).logoUrl());
    }

    @Test
    void rechazaActualizarConElRucDeOtraEmpresaSinModificarLaActual() {
        Empresa empresa = new Empresa();
        empresa.setId(4L);
        empresa.setNombre("Nombre original");
        when(empresaRepository.findOneById(4L)).thenReturn(Optional.of(empresa));
        when(empresaRepository.existsByRucAndIdNot("RUC-002", 4L)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.actualizar(4L, request("RUC-002")));

        assertEquals("Nombre original", empresa.getNombre());
        verify(empresaRepository, never()).saveAndFlush(any(Empresa.class));
    }

    @Test
    void informaCuandoNoExisteLaEmpresa() {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
    }

    @Test
    void convierteEnConflictoElRucDuplicadoDetectadoPorPostgres() {
        ConstraintViolationException constraint = mock(ConstraintViolationException.class);
        when(constraint.getConstraintName()).thenReturn("empresa_ruc_key");
        when(empresaRepository.saveAndFlush(any(Empresa.class)))
            .thenThrow(new DataIntegrityViolationException("Conflicto de escritura", constraint));

        assertThrows(ConflictoException.class, () -> service.crear(request("RUC-001")));
    }

    @Test
    void noDisfrazaOtrosErroresDeIntegridadComoRucDuplicado() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("Otro error");
        when(empresaRepository.saveAndFlush(any(Empresa.class))).thenThrow(failure);

        DataIntegrityViolationException actual = assertThrows(
            DataIntegrityViolationException.class, () -> service.crear(request("RUC-001")));

        assertSame(failure, actual);
    }

    private EmpresaRequest request(String ruc) {
        return new EmpresaRequest("Boutique", ruc, null, "Managua", null, null,
            null, null, new BigDecimal("15.00"));
    }
}
