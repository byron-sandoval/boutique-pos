package com.boutique.backend.proveedor.controller;

import java.util.List;
import java.util.stream.Stream;

import com.boutique.backend.proveedor.dto.ProveedorRequest;
import com.boutique.backend.proveedor.dto.ProveedorResponse;
import com.boutique.backend.proveedor.service.ProveedorService;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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

// Contrato HTTP aislado: no inicia Spring Boot, seguridad ni conexion a datos.
@ExtendWith(MockitoExtension.class)
class ProveedorControllerTest {

    @Mock private ProveedorService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ProveedorController(service))
            .setValidator(validator)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void creaConDatosOpcionalesAusentesYDevuelve201() throws Exception {
        when(service.crear(any(ProveedorRequest.class))).thenReturn(response(true));

        mvc.perform(post("/api/proveedores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Proveedor\",\"activo\":true}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/proveedores/1"))
            .andExpect(jsonPath("$.id").value(1));
    }

    @ParameterizedTest
    @MethodSource("solicitudesInvalidas")
    void rechazaDatosInvalidosAntesDeGuardar(String body) throws Exception {
        mvc.perform(post("/api/proveedores").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void informaProveedorInexistente() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe."));

        mvc.perform(get("/api/proveedores/99")).andExpect(status().isNotFound());
    }

    @Test
    void rechazaIdsInvalidos() throws Exception {
        mvc.perform(get("/api/proveedores/0")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/proveedores/-1")).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void validaElCorreoTambienAlActualizar() throws Exception {
        mvc.perform(put("/api/proveedores/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Proveedor\",\"email\":\"invalido\",\"activo\":true}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void listaConPaginacion() throws Exception {
        when(service.listar(any(Pageable.class))).thenAnswer(invocation ->
            new PageImpl<>(List.of(response(true)), invocation.getArgument(0), 3));

        mvc.perform(get("/api/proveedores").param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.number").value(1))
            .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void permiteActivarYDesactivarConPatch(boolean activo) throws Exception {
        when(service.cambiarEstado(1L, activo)).thenReturn(response(activo));

        mvc.perform(patch("/api/proveedores/1/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":" + activo + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(activo));
    }

    @Test
    void exigeEstadoExplicito() throws Exception {
        mvc.perform(patch("/api/proveedores/1/estado").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deleteDesactivaYDevuelve204() throws Exception {
        when(service.cambiarEstado(1L, false)).thenReturn(response(false));

        mvc.perform(delete("/api/proveedores/1")).andExpect(status().isNoContent());

        verify(service).cambiarEstado(1L, false);
    }

    @Test
    void deleteDevuelve404ParaProveedorInexistente() throws Exception {
        when(service.cambiarEstado(99L, false)).thenThrow(new RecursoNoEncontradoException("No existe."));

        mvc.perform(delete("/api/proveedores/99")).andExpect(status().isNotFound());
    }

    private static Stream<String> solicitudesInvalidas() {
        return Stream.of(
            "{\"nombre\":\" \",\"activo\":true}",
            "{\"nombre\":\"Proveedor\"}",
            "{\"nombre\":\"Proveedor\",\"email\":\"no-es-correo\",\"activo\":true}",
            "{\"nombre\":\"Proveedor\",\"direccion\":\"" + "X".repeat(251) + "\",\"activo\":true}",
            "{\"nombre\":\"Proveedor\",\"nombreContacto\":\"" + "X".repeat(101) + "\",\"activo\":true}",
            "{\"nombre\":\"Proveedor\",\"telefono\":\"" + "1".repeat(21) + "\",\"activo\":true}"
        );
    }

    private ProveedorResponse response(boolean activo) {
        return new ProveedorResponse(1L, "Proveedor", null, null, null, null, activo);
    }
}
