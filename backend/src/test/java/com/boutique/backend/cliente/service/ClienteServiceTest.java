package com.boutique.backend.cliente.service;

import java.util.Optional;

import com.boutique.backend.cliente.dto.ClienteRequest;
import com.boutique.backend.cliente.entity.Cliente;
import com.boutique.backend.cliente.mapper.ClienteMapper;
import com.boutique.backend.cliente.repository.ClienteRepository;
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
import org.springframework.data.repository.query.parser.PartTree;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class ClienteServiceTest {

    @Mock private ClienteRepository repository;
    private ClienteService service;

    @BeforeEach
    void setUp() {
        service = new ClienteService(repository, new ClienteMapper());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void admiteClientesSinDocumentosSinGuardarCadenasVacias(String documento) {
        when(repository.saveAndFlush(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente cliente = invocation.getArgument(0);
            cliente.setId(1L);
            return cliente;
        });

        var response = service.crear(request(documento, documento));

        assertNull(response.cedula());
        assertNull(response.pasaporte());
        assertEquals(1L, response.id());
        verify(repository, never()).existsByCedula(anyString());
        verify(repository, never()).existsByPasaporte(anyString());
    }

    @Test
    void rechazaCedulaDuplicadaNormalizada() {
        when(repository.existsByCedula("CED-1")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.crear(request(" CED-1 ", null)));

        verify(repository, never()).saveAndFlush(any(Cliente.class));
    }

    @Test
    void rechazaPasaporteDuplicadoNormalizado() {
        when(repository.existsByPasaporte("PAS-1")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.crear(request(null, " PAS-1 ")));

        verify(repository, never()).saveAndFlush(any(Cliente.class));
    }

    @Test
    void actualizarExcluyeElPropioClienteSinCambiarSuId() {
        Cliente cliente = cliente();
        when(repository.findById(1L)).thenReturn(Optional.of(cliente));
        when(repository.saveAndFlush(cliente)).thenReturn(cliente);

        var response = service.actualizar(1L, request("CED-1", "PAS-1"));

        assertEquals(1L, response.id());
        verify(repository).existsByCedulaAndIdNot("CED-1", 1L);
        verify(repository).existsByPasaporteAndIdNot("PAS-1", 1L);
        verify(repository, never()).existsByCedula(anyString());
        verify(repository, never()).existsByPasaporte(anyString());
    }

    @Test
    void unDuplicadoNoModificaLaEntidadExistente() {
        Cliente cliente = cliente();
        when(repository.findById(1L)).thenReturn(Optional.of(cliente));
        when(repository.existsByPasaporteAndIdNot("PAS-OTRO", 1L)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.actualizar(1L, request(null, "PAS-OTRO")));

        assertEquals("Original", cliente.getNombre());
        assertEquals("CED-1", cliente.getCedula());
        verify(repository, never()).saveAndFlush(any(Cliente.class));
    }

    @Test
    void desactivarEsIdempotenteYConservaElCliente() {
        Cliente cliente = cliente();
        when(repository.findById(1L)).thenReturn(Optional.of(cliente));
        when(repository.saveAndFlush(cliente)).thenReturn(cliente);

        service.cambiarEstado(1L, false);
        var response = service.cambiarEstado(1L, false);

        assertFalse(response.activo());
        assertEquals("CED-1", response.cedula());
        assertEquals(1L, response.id());
        verify(repository, never()).delete(any(Cliente.class));
        verify(repository, never()).deleteById(1L);
    }

    @Test
    void noGuardaSiElClienteAActualizarNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
            () -> service.actualizar(99L, request(null, null)));

        verify(repository, never()).saveAndFlush(any(Cliente.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cliente_cedula_key", "cliente_pasaporte_key"})
    void traduceDuplicadosDetectadosEnEscriturasSimultaneas(String constraintName) {
        ConstraintViolationException constraint = mock(ConstraintViolationException.class);
        when(constraint.getConstraintName()).thenReturn(constraintName);
        when(repository.saveAndFlush(any(Cliente.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicado", constraint));

        assertThrows(ConflictoException.class, () -> service.crear(request(null, null)));
    }

    @Test
    void noDisfrazaOtrosErroresDeIntegridad() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("Otro error");
        when(repository.saveAndFlush(any(Cliente.class))).thenThrow(failure);

        assertSame(failure, assertThrows(DataIntegrityViolationException.class,
            () -> service.crear(request(null, null))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"existsByCedula", "existsByCedulaAndIdNot", "existsByPasaporte", "existsByPasaporteAndIdNot"})
    void consultasDerivadasUsanPropiedadesExistentes(String metodo) {
        assertDoesNotThrow(() -> new PartTree(metodo, Cliente.class));
    }

    private ClienteRequest request(String cedula, String pasaporte) {
        return new ClienteRequest(cedula, pasaporte, "Cliente de prueba", null, null, true);
    }

    private Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Original");
        cliente.setCedula("CED-1");
        return cliente;
    }
}
