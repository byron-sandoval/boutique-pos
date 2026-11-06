package com.boutique.backend.producto.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import com.boutique.backend.producto.dto.CategoriaRequest;
import com.boutique.backend.producto.dto.CategoriaResponse;
import com.boutique.backend.producto.dto.ProductoRequest;
import com.boutique.backend.producto.dto.ProductoResponse;
import com.boutique.backend.producto.dto.UnidadMedidaRequest;
import com.boutique.backend.producto.dto.UnidadMedidaResponse;
import com.boutique.backend.producto.dto.VarianteProductoRequest;
import com.boutique.backend.producto.dto.VarianteProductoResponse;
import com.boutique.backend.producto.service.CategoriaService;
import com.boutique.backend.producto.service.ProductoService;
import com.boutique.backend.producto.service.UnidadMedidaService;
import com.boutique.backend.producto.service.VarianteProductoService;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Contrato HTTP aislado, sin Spring Boot, seguridad, base de datos ni migraciones.
@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock private CategoriaService categorias;
    @Mock private UnidadMedidaService unidades;
    @Mock private ProductoService productos;
    @Mock private VarianteProductoService variantes;

    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new CategoriaController(categorias),
                new UnidadMedidaController(unidades), new ProductoController(productos),
                new VarianteProductoController(variantes))
            .setValidator(validator)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @ParameterizedTest
    @MethodSource("altas")
    void crearDevuelve201ConUbicacion(String recurso, String body) throws Exception {
        switch (recurso) {
            case "categorias" -> when(categorias.crear(any(CategoriaRequest.class))).thenReturn(categoria(true));
            case "unidades-medida" -> when(unidades.crear(any(UnidadMedidaRequest.class))).thenReturn(unidad(true));
            case "productos" -> when(productos.crear(any(ProductoRequest.class))).thenReturn(producto(true));
            case "variantes-producto" -> when(variantes.crear(any(VarianteProductoRequest.class))).thenReturn(variante(true));
            default -> throw new IllegalArgumentException(recurso);
        }

        mvc.perform(post("/api/" + recurso).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/" + recurso + "/1"))
            .andExpect(jsonPath("$.id").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"categorias", "unidades-medida", "productos", "variantes-producto"})
    void desactivarAceptaFalseSinEliminar(String recurso) throws Exception {
        switch (recurso) {
            case "categorias" -> when(categorias.cambiarEstado(1L, false)).thenReturn(categoria(false));
            case "unidades-medida" -> when(unidades.cambiarEstado(1L, false)).thenReturn(unidad(false));
            case "productos" -> when(productos.cambiarEstado(1L, false)).thenReturn(producto(false));
            case "variantes-producto" -> when(variantes.cambiarEstado(1L, false)).thenReturn(variante(false));
            default -> throw new IllegalArgumentException(recurso);
        }

        mvc.perform(patch("/api/" + recurso + "/1/estado")
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"categorias", "unidades-medida", "productos", "variantes-producto"})
    void elEstadoEsObligatorio(String recurso) throws Exception {
        mvc.perform(patch("/api/" + recurso + "/1/estado")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(categorias, unidades, productos, variantes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "1.001", "10000000000.00"})
    void rechazaPreciosQueNoCabenEnElEsquema(String precio) throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                .content(productoJson(precio)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(productos);
    }

    @Test
    void rechazaUnSkuVacioAunqueElProductoSeaValido() throws Exception {
        mvc.perform(post("/api/variantes-producto").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"sku\":\" \",\"activo\":true}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(variantes);
    }

    @Test
    void productosSeFiltranPorCategoriaConPaginacion() throws Exception {
        when(productos.listarPorCategoria(eq(2L), any(Pageable.class))).thenAnswer(invocation ->
            new PageImpl<>(List.of(producto(true)), invocation.getArgument(1), 3));

        mvc.perform(get("/api/productos").param("categoriaId", "2").param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].categoriaId").value(2))
            .andExpect(jsonPath("$.page.number").value(1))
            .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void variantesSeFiltranPorProductoSinExponerLaEntidadCompleta() throws Exception {
        when(variantes.listarPorProducto(eq(1L), any(Pageable.class))).thenAnswer(invocation ->
            new PageImpl<>(List.of(variante(true)), invocation.getArgument(1), 1));

        mvc.perform(get("/api/variantes-producto").param("productoId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].productoId").value(1))
            .andExpect(jsonPath("$.content[0].producto").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"productos,categoriaId", "variantes-producto,productoId"})
    void rechazaFiltrosConIdentificadoresInvalidos(String recurso, String filtro) throws Exception {
        mvc.perform(get("/api/" + recurso).param(filtro, "0"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(productos, variantes);
    }

    @Test
    void devuelve409SiLaCombinacionDeVarianteYaExiste() throws Exception {
        when(variantes.crear(any(VarianteProductoRequest.class)))
            .thenThrow(new ConflictoException("La variante ya existe."));

        mvc.perform(post("/api/variantes-producto").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"sku\":\"SKU-1\",\"activo\":true}"))
            .andExpect(status().isConflict());
    }

    @Test
    void devuelve404SiElProductoNoExiste() throws Exception {
        when(productos.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe."));

        mvc.perform(get("/api/productos/99")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"categorias", "unidades-medida", "productos", "variantes-producto"})
    void deleteDesactivaElRegistroYPermiteRepetirLaSolicitud(String recurso) throws Exception {
        switch (recurso) {
            case "categorias" -> when(categorias.cambiarEstado(1L, false)).thenReturn(categoria(false));
            case "unidades-medida" -> when(unidades.cambiarEstado(1L, false)).thenReturn(unidad(false));
            case "productos" -> when(productos.cambiarEstado(1L, false)).thenReturn(producto(false));
            case "variantes-producto" -> when(variantes.cambiarEstado(1L, false)).thenReturn(variante(false));
            default -> throw new IllegalArgumentException(recurso);
        }

        mvc.perform(delete("/api/" + recurso + "/1")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/" + recurso + "/1")).andExpect(status().isNoContent());

        switch (recurso) {
            case "categorias" -> verify(categorias, times(2)).cambiarEstado(1L, false);
            case "unidades-medida" -> verify(unidades, times(2)).cambiarEstado(1L, false);
            case "productos" -> verify(productos, times(2)).cambiarEstado(1L, false);
            case "variantes-producto" -> verify(variantes, times(2)).cambiarEstado(1L, false);
            default -> throw new IllegalArgumentException(recurso);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"categorias", "unidades-medida", "productos", "variantes-producto"})
    void deleteRechazaIdentificadoresInvalidos(String recurso) throws Exception {
        mvc.perform(delete("/api/" + recurso + "/0")).andExpect(status().isBadRequest());

        verifyNoInteractions(categorias, unidades, productos, variantes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"categorias", "unidades-medida", "productos", "variantes-producto"})
    void deleteDevuelve404ParaUnRegistroInexistente(String recurso) throws Exception {
        RecursoNoEncontradoException error = new RecursoNoEncontradoException("No existe.");
        switch (recurso) {
            case "categorias" -> when(categorias.cambiarEstado(99L, false)).thenThrow(error);
            case "unidades-medida" -> when(unidades.cambiarEstado(99L, false)).thenThrow(error);
            case "productos" -> when(productos.cambiarEstado(99L, false)).thenThrow(error);
            case "variantes-producto" -> when(variantes.cambiarEstado(99L, false)).thenThrow(error);
            default -> throw new IllegalArgumentException(recurso);
        }

        mvc.perform(delete("/api/" + recurso + "/99")).andExpect(status().isNotFound());
    }

    private static Stream<Arguments> altas() {
        return Stream.of(
            Arguments.of("categorias", "{\"nombre\":\"Ropa\",\"activo\":true}"),
            Arguments.of("unidades-medida", "{\"nombre\":\"Unidad\",\"activo\":true}"),
            Arguments.of("productos", productoJson("100.00")),
            Arguments.of("variantes-producto", "{\"productoId\":1,\"sku\":\"SKU-1\",\"activo\":true}")
        );
    }

    private static String productoJson(String precio) {
        return """
            {"categoriaId":2,"unidadMedidaId":3,"codigo":"P-1","nombre":"Camisa",
             "precio":%s,"costo":50.00,"activo":true}
            """.formatted(precio);
    }

    private CategoriaResponse categoria(boolean activo) {
        return new CategoriaResponse(1L, "Ropa", null, activo);
    }

    private UnidadMedidaResponse unidad(boolean activo) {
        return new UnidadMedidaResponse(1L, "Unidad", "ud", activo);
    }

    private ProductoResponse producto(boolean activo) {
        return new ProductoResponse(1L, 2L, 3L, "P-1", "Camisa", null, null,
            new BigDecimal("100.00"), new BigDecimal("50.00"), null, null, activo);
    }

    private VarianteProductoResponse variante(boolean activo) {
        return new VarianteProductoResponse(1L, 1L, "M", "Rojo", "SKU-1", null, activo);
    }
}
