package com.boutique.backend.empresa.controller;

import java.math.BigDecimal;
import java.util.List;

import com.boutique.backend.empresa.dto.EmpresaRequest;
import com.boutique.backend.empresa.dto.EmpresaResponse;
import com.boutique.backend.empresa.dto.SucursalResponse;
import com.boutique.backend.empresa.service.EmpresaService;
import com.boutique.backend.empresa.service.SucursalService;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas HTTP aisladas: no cargan Spring Boot, filtros de seguridad ni repositorios.
@ExtendWith(MockitoExtension.class)
class EmpresaSucursalControllerTest {

    @Mock
    private EmpresaService empresaService;

    @Mock
    private SucursalService sucursalService;

    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(
                new EmpresaController(empresaService), new SucursalController(sucursalService))
            .setValidator(validator)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void crearEmpresaDevuelve201ConLaUbicacionDelRecurso() throws Exception {
        when(empresaService.crear(any(EmpresaRequest.class))).thenReturn(empresa());

        mvc.perform(post("/api/empresas").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Boutique","direccion":"Managua","porcentajeIva":15.00}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/empresas/1"))
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void rechazaIvaFueraDelRangoAntesDeLlamarAlServicio() throws Exception {
        mvc.perform(post("/api/empresas").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Boutique","direccion":"Managua","porcentajeIva":100.01}
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(empresaService);
    }

    @Test
    void devuelve404CuandoLaEmpresaNoExiste() throws Exception {
        when(empresaService.obtenerPorId(99L))
            .thenThrow(new RecursoNoEncontradoException("La empresa no existe."));

        mvc.perform(get("/api/empresas/99")).andExpect(status().isNotFound());
    }

    @Test
    void devuelve409ParaUnRucDuplicado() throws Exception {
        when(empresaService.crear(any(EmpresaRequest.class)))
            .thenThrow(new ConflictoException("El RUC ya existe."));

        mvc.perform(post("/api/empresas").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Boutique","ruc":"RUC-001","direccion":"Managua","porcentajeIva":15}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void rechazaUnIdentificadorDeEmpresaNegativo() throws Exception {
        mvc.perform(get("/api/empresas/-1")).andExpect(status().isBadRequest());

        verifyNoInteractions(empresaService);
    }

    @Test
    void validaElBodyDeActualizacionAdemasDelId() throws Exception {
        mvc.perform(put("/api/empresas/1").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":" ","direccion":"Managua","porcentajeIva":15}
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(empresaService);
    }

    @Test
    void listaSucursalesPorEmpresaConPaginacionEnElJson() throws Exception {
        when(sucursalService.listarPorEmpresa(eq(1L), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1);
            return new PageImpl<>(List.of(sucursal(true)), pageable, 3);
        });

        mvc.perform(get("/api/sucursales").param("empresaId", "1").param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].empresaId").value(1))
            .andExpect(jsonPath("$.page.number").value(1))
            .andExpect(jsonPath("$.page.size").value(2))
            .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void rechazaUnFiltroDeEmpresaConIdInvalido() throws Exception {
        mvc.perform(get("/api/sucursales").param("empresaId", "0"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(sucursalService);
    }

    @Test
    void rechazaUnCambioDeEstadoSinValor() throws Exception {
        mvc.perform(patch("/api/sucursales/2/estado").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(sucursalService);
    }

    @Test
    void aceptaFalseComoEstadoValidoParaDesactivar() throws Exception {
        when(sucursalService.cambiarEstado(2L, false)).thenReturn(sucursal(false));

        mvc.perform(patch("/api/sucursales/2/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"activa\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activa").value(false));

        verify(sucursalService).cambiarEstado(2L, false);
    }

    @Test
    void noExponeEliminacionFisicaDeSucursales() throws Exception {
        mvc.perform(delete("/api/sucursales/2")).andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(sucursalService);
    }

    private EmpresaResponse empresa() {
        return new EmpresaResponse(1L, "Boutique", "RUC-001", null, "Managua",
            null, null, null, null, new BigDecimal("15.00"));
    }

    private SucursalResponse sucursal(boolean activa) {
        return new SucursalResponse(2L, 1L, "Centro", "Managua", null, activa);
    }
}
