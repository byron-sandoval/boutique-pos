package com.boutique.backend.producto.service;

import java.util.Optional;

import com.boutique.backend.producto.dto.VarianteProductoRequest;
import com.boutique.backend.producto.entity.Producto;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.producto.mapper.VarianteProductoMapper;
import com.boutique.backend.producto.repository.ProductoRepository;
import com.boutique.backend.producto.repository.VarianteProductoRepository;
import com.boutique.backend.shared.exception.ConflictoException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VarianteProductoServiceTest {

    @Mock private VarianteProductoRepository variantes;
    @Mock private ProductoRepository productos;
    private VarianteProductoService service;

    @BeforeEach
    void setUp() {
        service = new VarianteProductoService(variantes, productos, new VarianteProductoMapper());
    }

    @Test
    void requiereQueElProductoExistaAntesDeCrearUnaVariante() {
        when(productos.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(request("SKU-1", null, null, null)));

        verifyNoInteractions(variantes);
    }

    @Test
    void detectaSkuDuplicadoNormalizado() {
        when(productos.findById(1L)).thenReturn(Optional.of(producto()));
        when(variantes.existsBySku("SKU-1")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.crear(request(" SKU-1 ", "M", "Rojo", null)));

        verify(variantes, never()).saveAndFlush(any(VarianteProducto.class));
    }

    @Test
    void detectaCodigoDeBarrasDuplicado() {
        when(productos.findById(1L)).thenReturn(Optional.of(producto()));
        when(variantes.existsByCodigoBarras("12345")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.crear(request("SKU-1", "M", "Rojo", " 12345 ")));

        verify(variantes, never()).saveAndFlush(any(VarianteProducto.class));
    }

    @Test
    void detectaCombinacionDuplicadaAunqueNoHayaTallaNiColor() {
        when(productos.findById(1L)).thenReturn(Optional.of(producto()));
        when(variantes.existsByProductoIdAndTallaAndColor(1L, null, null)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.crear(request("SKU-1", " ", null, " ")));

        verify(variantes, never()).existsByCodigoBarras(anyString());
        verify(variantes, never()).saveAndFlush(any(VarianteProducto.class));
    }

    @Test
    void noGuardaCadenasVaciasComoTallaColorOCodigoDeBarras() {
        when(productos.findById(1L)).thenReturn(Optional.of(producto()));
        when(variantes.saveAndFlush(any(VarianteProducto.class))).thenAnswer(invocation -> {
            VarianteProducto variante = invocation.getArgument(0);
            variante.setId(2L);
            return variante;
        });

        var response = service.crear(request("SKU-1", " ", "", " "));

        assertNull(response.talla());
        assertNull(response.color());
        assertNull(response.codigoBarras());
        assertEquals(1L, response.productoId());
        verify(variantes, never()).existsByCodigoBarras(anyString());
    }

    @Test
    void actualizarExcluyeLaPropiaVarianteEnTodasLasComprobaciones() {
        VarianteProducto variante = variante();
        when(variantes.findById(2L)).thenReturn(Optional.of(variante));
        when(variantes.saveAndFlush(variante)).thenReturn(variante);

        var response = service.actualizar(2L, request("SKU-1", "M", "Rojo", "12345"));

        assertEquals(2L, response.id());
        verify(variantes).existsBySkuAndIdNot("SKU-1", 2L);
        verify(variantes).existsByCodigoBarrasAndIdNot("12345", 2L);
        verify(variantes).existsByProductoIdAndTallaAndColorAndIdNot(1L, "M", "Rojo", 2L);
        verify(variantes, never()).existsBySku(anyString());
    }

    @Test
    void impideReasignarUnaVarianteAUnProductoDiferente() {
        VarianteProducto variante = variante();
        when(variantes.findById(2L)).thenReturn(Optional.of(variante));

        assertThrows(ConflictoException.class, () -> service.actualizar(2L,
            new VarianteProductoRequest(99L, "M", "Rojo", "OTRO-SKU", null, true)));

        assertEquals(1L, variante.getProducto().getId());
        assertEquals("SKU-1", variante.getSku());
        verify(variantes, never()).saveAndFlush(any(VarianteProducto.class));
        verifyNoInteractions(productos);
    }

    @Test
    void desactivarConservaLaRelacionYNoEliminaElRegistro() {
        VarianteProducto variante = variante();
        when(variantes.findById(2L)).thenReturn(Optional.of(variante));
        when(variantes.saveAndFlush(variante)).thenReturn(variante);

        var response = service.cambiarEstado(2L, false);

        assertFalse(response.activo());
        assertEquals(1L, response.productoId());
        verify(variantes, never()).delete(any(VarianteProducto.class));
    }

    private Producto producto() {
        Producto producto = new Producto();
        producto.setId(1L);
        return producto;
    }

    private VarianteProducto variante() {
        VarianteProducto variante = new VarianteProducto();
        variante.setId(2L);
        variante.setProducto(producto());
        variante.setSku("SKU-1");
        return variante;
    }

    private VarianteProductoRequest request(String sku, String talla, String color, String barras) {
        return new VarianteProductoRequest(1L, talla, color, sku, barras, true);
    }
}
