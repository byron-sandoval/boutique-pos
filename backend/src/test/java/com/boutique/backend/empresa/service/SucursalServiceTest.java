package com.boutique.backend.empresa.service;

import java.util.List;
import java.util.Optional;

import com.boutique.backend.empresa.dto.SucursalRequest;
import com.boutique.backend.empresa.dto.SucursalResponse;
import com.boutique.backend.empresa.entity.Empresa;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.mapper.SucursalMapper;
import com.boutique.backend.empresa.repository.EmpresaRepository;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.venta.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SucursalServiceTest {

    @Mock
    private SucursalRepository sucursalRepository;

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private VentaRepository ventaRepository;

    private SucursalService service;

    @BeforeEach
    void setUp() {
        service = new SucursalService(sucursalRepository, empresaRepository, new SucursalMapper(), ventaRepository);
    }

    @Test
    void impideCrearUnaSucursalParaUnaEmpresaInexistente() {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
            () -> service.crear(new SucursalRequest(99L, "Centro", "Managua", null, true)));

        verifyNoInteractions(sucursalRepository);
    }

    @Test
    void informaCuandoNoExisteLaSucursalQueSeQuiereActualizar() {
        when(sucursalRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
            () -> service.actualizar(99L, new SucursalRequest(1L, "Centro", "Managua", null, true)));

        verifyNoInteractions(empresaRepository);
        verify(sucursalRepository, never()).save(any(Sucursal.class));
    }

    @Test
    void noModificaLaSucursalSiLaNuevaEmpresaNoExiste() {
        Sucursal sucursal = sucursal(empresa(1L));
        when(sucursalRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(sucursal));
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
            () -> service.actualizar(2L, new SucursalRequest(99L, "Otro nombre", "Otra direccion", null, false)));

        assertEquals("Centro", sucursal.getNombre());
        assertEquals(1L, sucursal.getEmpresa().getId());
        verify(sucursalRepository, never()).save(any(Sucursal.class));
    }

    @Test
    void actualizaUsandoLaEmpresaResueltaSinCambiarElIdDeSucursal() {
        Sucursal sucursal = sucursal(empresa(1L));
        Empresa empresa = empresa(3L);
        when(sucursalRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(sucursal));
        when(empresaRepository.findById(3L)).thenReturn(Optional.of(empresa));
        when(sucursalRepository.save(sucursal)).thenReturn(sucursal);

        SucursalResponse response = service.actualizar(
            2L, new SucursalRequest(3L, "Nueva sucursal", "Leon", null, true));

        assertEquals(2L, response.id());
        assertEquals(3L, response.empresaId());
        assertSame(empresa, sucursal.getEmpresa());
    }

    @Test
    void desactivaLaSucursalSinEliminarlaNiCambiarSuEmpresa() {
        Sucursal sucursal = sucursal(empresa(1L));
        when(sucursalRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(sucursal));
        when(sucursalRepository.save(sucursal)).thenReturn(sucursal);

        SucursalResponse response = service.cambiarEstado(2L, false);

        assertFalse(response.activa());
        assertEquals(2L, response.id());
        assertEquals(1L, response.empresaId());
        assertEquals("Centro", response.nombre());
        verify(ventaRepository).existsBySucursalId(2L);
        verify(sucursalRepository, never()).delete(any(Sucursal.class));
        verify(sucursalRepository, never()).deleteById(anyLong());
    }

    @Test
    void rechazaDesactivarPorEstadoUnaSucursalConVentas() {
        Sucursal sucursal = sucursal(empresa(1L));
        when(sucursalRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(sucursal));
        when(ventaRepository.existsBySucursalId(2L)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.cambiarEstado(2L, false));

        assertTrue(sucursal.getActiva());
        verify(sucursalRepository, never()).save(any(Sucursal.class));
    }

    @Test
    void rechazaDesactivarPorEdicionUnaSucursalConVentasSinCambiarSusDatos() {
        Empresa empresa = empresa(1L);
        Sucursal sucursal = sucursal(empresa);
        when(sucursalRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(sucursal));
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(ventaRepository.existsBySucursalId(2L)).thenReturn(true);

        var request = new SucursalRequest(1L, "Otro nombre", "León", null, false);
        assertThrows(ConflictoException.class, () -> service.actualizar(2L, request));

        assertTrue(sucursal.getActiva());
        assertEquals("Centro", sucursal.getNombre());
        verify(sucursalRepository, never()).save(any(Sucursal.class));
    }

    @Test
    void permiteReactivarUnaSucursalSinConsultarSusVentas() {
        Sucursal sucursal = sucursal(empresa(1L));
        sucursal.setActiva(false);
        when(sucursalRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(sucursal));
        when(sucursalRepository.save(sucursal)).thenReturn(sucursal);

        assertTrue(service.cambiarEstado(2L, true).activa());
        verifyNoInteractions(ventaRepository);
    }

    @Test
    void impideListarSucursalesDeUnaEmpresaInexistente() {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
            () -> service.listarPorEmpresa(99L, PageRequest.of(0, 20)));

        verify(sucursalRepository, never()).findByEmpresaId(anyLong(), any(Pageable.class));
    }

    @Test
    void conservaLaPaginacionAlConsultarPorEmpresa() {
        Empresa empresa = empresa(1L);
        Pageable pageable = PageRequest.of(1, 2);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(sucursalRepository.findByEmpresaId(1L, pageable))
            .thenReturn(new PageImpl<>(List.of(sucursal(empresa)), pageable, 3));

        Page<SucursalResponse> response = service.listarPorEmpresa(1L, pageable);

        assertEquals(1, response.getNumber());
        assertEquals(3, response.getTotalElements());
        assertEquals(1L, response.getContent().getFirst().empresaId());
    }

    private Empresa empresa(Long id) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        return empresa;
    }

    private Sucursal sucursal(Empresa empresa) {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(2L);
        sucursal.setEmpresa(empresa);
        sucursal.setNombre("Centro");
        sucursal.setDireccion("Managua");
        return sucursal;
    }
}
