package com.boutique.backend.inventario.service;

import java.util.Optional;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.inventario.dto.InventarioCreateRequest;
import com.boutique.backend.inventario.dto.InventarioLimitesRequest;
import com.boutique.backend.inventario.entity.Inventario;
import com.boutique.backend.inventario.mapper.InventarioMapper;
import com.boutique.backend.inventario.repository.InventarioRepository;
import com.boutique.backend.producto.entity.Producto;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.producto.repository.VarianteProductoRepository;
import com.boutique.backend.shared.exception.ConflictoException;
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
class InventarioServiceTest {
    @Mock InventarioRepository repository;
    @Mock SucursalRepository sucursales;
    @Mock VarianteProductoRepository variantes;
    InventarioService service;
    Sucursal sucursal;
    VarianteProducto variante;

    @BeforeEach
    void preparar() {
        service = new InventarioService(repository, sucursales, variantes, new InventarioMapper());
        sucursal = new Sucursal();
        sucursal.setId(1L);
        variante = new VarianteProducto();
        variante.setId(4L);
        variante.setProducto(new Producto());
    }

    private void relacionesActivas() {
        when(sucursales.findById(1L)).thenReturn(Optional.of(sucursal));
        when(variantes.findById(4L)).thenReturn(Optional.of(variante));
    }

    @Test
    void creaExistenciaSinInventarStockInicial() {
        relacionesActivas();
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var respuesta = service.crear(new InventarioCreateRequest(4L, 1L, 3, 20));
        assertEquals(0, respuesta.stock());
        assertEquals(3, respuesta.stockMinimo());
    }

    @Test
    void actualizarLimitesConservaStockYRelaciones() {
        Inventario inventario = new Inventario();
        inventario.setId(9L);
        inventario.setSucursal(sucursal);
        inventario.setVariante(variante);
        inventario.setStock(27);
        when(repository.findByIdForUpdate(9L)).thenReturn(Optional.of(inventario));
        when(repository.save(inventario)).thenReturn(inventario);
        var respuesta = service.actualizarLimites(9L, new InventarioLimitesRequest(5, null));
        assertEquals(27, respuesta.stock());
        assertEquals(4L, respuesta.varianteId());
        assertEquals(1L, respuesta.sucursalId());
        assertNull(respuesta.stockMaximo());
    }

    @Test
    void rechazaDuplicado() {
        relacionesActivas();
        when(repository.existsByVarianteIdAndSucursalId(4L, 1L)).thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.crear(new InventarioCreateRequest(4L, 1L, 0, null)));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void duplicadoConcurrenteSeTraduceAConflicto() {
        relacionesActivas();
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicado"));
        assertThrows(ConflictoException.class, () -> service.crear(new InventarioCreateRequest(4L, 1L, 0, null)));
    }

    @Test
    void rechazaVarianteInactiva() {
        relacionesActivas();
        variante.setActivo(false);
        assertThrows(ConflictoException.class, () -> service.crear(new InventarioCreateRequest(4L, 1L, 0, null)));
        verify(repository, never()).saveAndFlush(any());
    }
}
