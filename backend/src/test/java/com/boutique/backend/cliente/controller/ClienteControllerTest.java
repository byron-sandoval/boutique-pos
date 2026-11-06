package com.boutique.backend.cliente.controller;

import java.util.List;
import java.util.stream.Stream;

import com.boutique.backend.cliente.dto.ClienteRequest;
import com.boutique.backend.cliente.dto.ClienteResponse;
import com.boutique.backend.cliente.service.ClienteService;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

// Pruebas aisladas del contrato HTTP, sin base de datos ni filtros de seguridad.
@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock private ClienteService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ClienteController(service))
            .setValidator(validator)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void permiteCrearSinDocumentosYDevuelve201ConUbicacion() throws Exception {
        when(service.crear(any(ClienteRequest.class))).thenReturn(response(true));

        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Cliente\",\"activo\":true}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/clientes/1"))
            .andExpect(jsonPath("$.id").value(1));
    }

    @ParameterizedTest
    @MethodSource("solicitudesInvalidas")
    void validaLosCamposAntesDeLlamarAlServicio(String body) throws Exception {
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void devuelve409ParaDocumentoDuplicado() throws Exception {
        when(service.crear(any(ClienteRequest.class))).thenThrow(new ConflictoException("Documento duplicado."));

        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Cliente\",\"cedula\":\"CED-1\",\"activo\":true}"))
            .andExpect(status().isConflict());
    }

    @Test
    void devuelve404ParaClienteInexistente() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe."));

        mvc.perform(get("/api/clientes/99")).andExpect(status().isNotFound());
    }

    @Test
    void rechazaIdentificadoresInvalidos() throws Exception {
        mvc.perform(get("/api/clientes/0")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/clientes/-1")).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void validaTambienElBodyDeActualizacion() throws Exception {
        mvc.perform(put("/api/clientes/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\" \",\"activo\":true}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void listaConMetadatosDePaginacion() throws Exception {
        when(service.listar(any(Pageable.class))).thenAnswer(invocation ->
            new PageImpl<>(List.of(response(true)), invocation.getArgument(0), 3));

        mvc.perform(get("/api/clientes").param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.number").value(1))
            .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void permiteReactivarConPatch() throws Exception {
        when(service.cambiarEstado(1L, true)).thenReturn(response(true));

        mvc.perform(patch("/api/clientes/1/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void elEstadoNoPuedeEstarAusente() throws Exception {
        mvc.perform(patch("/api/clientes/1/estado").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deleteDesactivaYDevuelve204() throws Exception {
        when(service.cambiarEstado(1L, false)).thenReturn(response(false));

        mvc.perform(delete("/api/clientes/1")).andExpect(status().isNoContent());

        verify(service).cambiarEstado(1L, false);
    }

    @Test
    void deleteDevuelve404SiElClienteNoExiste() throws Exception {
        when(service.cambiarEstado(99L, false)).thenThrow(new RecursoNoEncontradoException("No existe."));

        mvc.perform(delete("/api/clientes/99")).andExpect(status().isNotFound());
    }

    private static Stream<String> solicitudesInvalidas() {
        return Stream.of(
            "{\"nombre\":\" \",\"activo\":true}",
            "{\"nombre\":\"Cliente\"}",
            "{\"nombre\":\"Cliente\",\"cedula\":\"" + "X".repeat(21) + "\",\"activo\":true}",
            "{\"nombre\":\"Cliente\",\"pasaporte\":\"" + "X".repeat(31) + "\",\"activo\":true}"
        );
    }

    private ClienteResponse response(boolean activo) {
        return new ClienteResponse(1L, null, null, "Cliente", null, null, activo);
    }
}
