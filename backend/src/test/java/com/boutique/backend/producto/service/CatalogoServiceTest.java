package com.boutique.backend.producto.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.boutique.backend.producto.dto.CategoriaRequest;
import com.boutique.backend.producto.dto.ProductoRequest;
import com.boutique.backend.producto.dto.UnidadMedidaRequest;
import com.boutique.backend.producto.entity.Categoria;
import com.boutique.backend.producto.entity.Producto;
import com.boutique.backend.producto.entity.UnidadMedida;
import com.boutique.backend.producto.mapper.CategoriaMapper;
import com.boutique.backend.producto.mapper.ProductoMapper;
import com.boutique.backend.producto.mapper.UnidadMedidaMapper;
import com.boutique.backend.producto.repository.CategoriaRepository;
import com.boutique.backend.producto.repository.ProductoRepository;
import com.boutique.backend.producto.repository.UnidadMedidaRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock private CategoriaRepository categorias;
    @Mock private UnidadMedidaRepository unidades;
    @Mock private ProductoRepository productos;

    private CategoriaService categoriaService;
    private UnidadMedidaService unidadService;
    private ProductoService productoService;

    @BeforeEach
    void setUp() {
        categoriaService = new CategoriaService(categorias, new CategoriaMapper());
        unidadService = new UnidadMedidaService(unidades, new UnidadMedidaMapper());
        productoService = new ProductoService(productos, categorias, unidades, new ProductoMapper());
    }

    @Test
    void rechazaCategoriaDuplicadaDespuesDeRecortarElNombre() {
        when(categorias.existsByNombre("Ropa")).thenReturn(true);

        assertThrows(ConflictoException.class,
            () -> categoriaService.crear(new CategoriaRequest(" Ropa ", null, true)));

        verify(categorias, never()).saveAndFlush(any(Categoria.class));
    }

    @Test
    void actualizarCategoriaExcluyeSuPropioIdDeLaComprobacion() {
        Categoria categoria = categoria();
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(categorias.saveAndFlush(categoria)).thenReturn(categoria);

        assertEquals(1L, categoriaService.actualizar(1L, new CategoriaRequest("Ropa", null, true)).id());

        verify(categorias).existsByNombreAndIdNot("Ropa", 1L);
        verify(categorias, never()).existsByNombre(anyString());
    }

    @Test
    void desactivarCategoriaConservaElRegistroYElNombre() {
        Categoria categoria = categoria();
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(categorias.saveAndFlush(categoria)).thenReturn(categoria);

        var response = categoriaService.cambiarEstado(1L, false);

        assertFalse(response.activo());
        assertEquals("Ropa", response.nombre());
        verify(categorias, never()).delete(any(Categoria.class));
    }

    @Test
    void rechazaNombreDuplicadoDeUnidadDeMedida() {
        when(unidades.existsByNombre("Unidad")).thenReturn(true);

        assertThrows(ConflictoException.class,
            () -> unidadService.crear(new UnidadMedidaRequest(" Unidad ", "ud", true)));

        verify(unidades, never()).saveAndFlush(any(UnidadMedida.class));
    }

    @Test
    void desactivarUnidadNoEliminaElRegistroYEsIdempotente() {
        UnidadMedida unidad = unidad();
        when(unidades.findById(2L)).thenReturn(Optional.of(unidad));
        when(unidades.saveAndFlush(unidad)).thenReturn(unidad);

        unidadService.cambiarEstado(2L, false);
        var response = unidadService.cambiarEstado(2L, false);

        assertFalse(response.activo());
        assertEquals(2L, response.id());
        assertEquals("Unidad", response.nombre());
        verify(unidades, never()).delete(any(UnidadMedida.class));
        verify(unidades, never()).deleteById(2L);
    }

    @Test
    void desactivarProductoConservaSusRelacionesYNoLoElimina() {
        Producto producto = new Producto();
        producto.setId(3L);
        producto.setCodigo("P-001");
        producto.setCategoria(categoria());
        producto.setUnidadMedida(unidad());
        when(productos.findById(3L)).thenReturn(Optional.of(producto));
        when(productos.saveAndFlush(producto)).thenReturn(producto);

        productoService.cambiarEstado(3L, false);
        var response = productoService.cambiarEstado(3L, false);

        assertFalse(response.activo());
        assertEquals(3L, response.id());
        assertEquals(1L, response.categoriaId());
        assertEquals(2L, response.unidadMedidaId());
        assertEquals("P-001", response.codigo());
        verify(productos, never()).delete(any(Producto.class));
        verify(productos, never()).deleteById(3L);
    }

    @Test
    void actualizarUnidadNoConsideraSuPropioNombreComoDuplicado() {
        UnidadMedida unidad = unidad();
        when(unidades.findById(2L)).thenReturn(Optional.of(unidad));
        when(unidades.saveAndFlush(unidad)).thenReturn(unidad);

        assertEquals(2L, unidadService.actualizar(2L, new UnidadMedidaRequest("Unidad", "ud", true)).id());

        verify(unidades).existsByNombreAndIdNot("Unidad", 2L);
        verify(unidades, never()).existsByNombre(anyString());
    }

    @Test
    void productoNoSeGuardaSiSuCategoriaNoExiste() {
        when(categorias.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> productoService.crear(request("P-001")));

        verifyNoInteractions(productos, unidades);
    }

    @Test
    void productoNoSeGuardaSiSuUnidadNoExiste() {
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria()));
        when(unidades.findById(2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> productoService.crear(request("P-001")));

        verifyNoInteractions(productos);
    }

    @Test
    void rechazaCodigoDeProductoDuplicadoConEspacios() {
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria()));
        when(unidades.findById(2L)).thenReturn(Optional.of(unidad()));
        when(productos.existsByCodigo("P-001")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> productoService.crear(request(" P-001 ")));

        verify(productos, never()).saveAndFlush(any(Producto.class));
    }

    @Test
    void actualizarProductoValidaTodoAntesDeModificarLaEntidad() {
        Producto producto = new Producto();
        producto.setId(3L);
        producto.setNombre("Nombre original");
        when(productos.findById(3L)).thenReturn(Optional.of(producto));
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria()));
        when(unidades.findById(2L)).thenReturn(Optional.of(unidad()));
        when(productos.existsByCodigoAndIdNot("P-002", 3L)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> productoService.actualizar(3L, request("P-002")));

        assertEquals("Nombre original", producto.getNombre());
        verify(productos, never()).saveAndFlush(any(Producto.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"categoria_nombre_key", "unidad_medida_nombre_key", "producto_codigo_key",
        "variante_producto_sku_key", "variante_producto_codigo_barras_key", "uq_variante_producto"})
    void reconoceConflictosDeUnicidadQueDetectaLaBase(String constraintName) {
        ConstraintViolationException constraint = mock(ConstraintViolationException.class);
        when(constraint.getConstraintName()).thenReturn(constraintName);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("Conflicto", constraint);

        assertThrows(ConflictoException.class, () -> CatalogoSupport.guardar(() -> { throw failure; }));
    }

    @Test
    void otrosErroresDeIntegridadNoSePresentanComoDuplicados() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("Otro problema");

        assertSame(failure, assertThrows(DataIntegrityViolationException.class,
            () -> CatalogoSupport.guardar(() -> { throw failure; })));
    }

    private Categoria categoria() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Ropa");
        return categoria;
    }

    private UnidadMedida unidad() {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setId(2L);
        unidad.setNombre("Unidad");
        return unidad;
    }

    private ProductoRequest request(String codigo) {
        return new ProductoRequest(1L, 2L, codigo, "Camisa", null, null,
            new BigDecimal("100.00"), new BigDecimal("50.00"), null, null, true);
    }
}
