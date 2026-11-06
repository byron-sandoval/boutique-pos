package com.boutique.backend.proveedor.service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.boutique.backend.proveedor.dto.ProveedorRequest;
import com.boutique.backend.proveedor.entity.Proveedor;
import com.boutique.backend.proveedor.mapper.ProveedorMapper;
import com.boutique.backend.proveedor.repository.ProveedorRepository;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock private ProveedorRepository repository;
    private ProveedorService service;

    @BeforeEach
    void setUp() {
        service = new ProveedorService(repository, new ProveedorMapper());
    }

    @Test
    void permiteProveedoresConElMismoNombreComoElEsquema() {
        AtomicLong ids = new AtomicLong();
        when(repository.save(any(Proveedor.class))).thenAnswer(invocation -> {
            Proveedor proveedor = invocation.getArgument(0);
            proveedor.setId(ids.incrementAndGet());
            return proveedor;
        });
        ProveedorRequest request = new ProveedorRequest("Proveedor", null, null, null, null, true);

        assertEquals(1L, service.crear(request).id());
        assertEquals(2L, service.crear(request).id());
    }

    @Test
    void normalizaOpcionalesVaciosSinGuardarCadenasEnBlanco() {
        when(repository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.crear(new ProveedorRequest(" Proveedor ", " ", null, "", "", true));

        assertEquals("Proveedor", response.nombre());
        assertNull(response.nombreContacto());
        assertNull(response.telefono());
        assertNull(response.email());
    }

    @Test
    void actualizarPermiteQuitarDatosOpcionalesSinCambiarElId() {
        Proveedor proveedor = proveedor();
        when(repository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(repository.save(proveedor)).thenReturn(proveedor);

        var response = service.actualizar(1L,
            new ProveedorRequest("Proveedor actualizado", null, null, null, null, true));

        assertEquals(1L, response.id());
        assertEquals("Proveedor actualizado", response.nombre());
        assertNull(response.email());
        assertNull(response.telefono());
    }

    @Test
    void noGuardaCuandoElProveedorAActualizarNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(99L,
            new ProveedorRequest("Proveedor", null, null, null, null, true)));

        verify(repository, never()).save(any(Proveedor.class));
    }

    @Test
    void desactivarEsIdempotenteYConservaDatosDeContacto() {
        Proveedor proveedor = proveedor();
        when(repository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(repository.save(proveedor)).thenReturn(proveedor);

        service.cambiarEstado(1L, false);
        var response = service.cambiarEstado(1L, false);

        assertFalse(response.activo());
        assertEquals(1L, response.id());
        assertEquals("contacto@example.com", response.email());
        assertEquals("22223333", response.telefono());
        verify(repository, never()).delete(any(Proveedor.class));
        verify(repository, never()).deleteById(1L);
    }

    @Test
    void noGuardaAlDesactivarUnProveedorInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarEstado(99L, false));

        verify(repository, never()).save(any(Proveedor.class));
    }

    private Proveedor proveedor() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor");
        proveedor.setEmail("contacto@example.com");
        proveedor.setTelefono("22223333");
        return proveedor;
    }
}
