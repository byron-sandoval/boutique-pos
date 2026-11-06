package com.boutique.backend.moneda.controller;

import java.util.stream.Stream;

import com.boutique.backend.moneda.dto.MonedaRequest;
import com.boutique.backend.moneda.dto.MonedaResponse;
import com.boutique.backend.moneda.service.MonedaService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MonedaControllerTest {
    @Mock MonedaService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void preparar() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new MonedaController(service)).setValidator(validator)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @AfterEach
    void cerrar() {
        validator.close();
    }

    private MonedaResponse respuesta() {
        return new MonedaResponse(1L, "NIO", "Córdoba", "C$", true);
    }

    @Test
    void creaYDevuelveUbicacion() throws Exception {
        when(service.crear(any())).thenReturn(respuesta());
        mvc.perform(post("/api/monedas").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigo\":\"nio\",\"nombre\":\"Córdoba\",\"simbolo\":\"C$\",\"activo\":true}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/monedas/1"))
            .andExpect(jsonPath("$.codigo").value("NIO"));
    }

    @ParameterizedTest
    @MethodSource("solicitudesInvalidas")
    void validaAlCrearYEditar(String body) throws Exception {
        mvc.perform(post("/api/monedas").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/monedas/1").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    static Stream<String> solicitudesInvalidas() {
        String base = "{\"codigo\":%s,\"nombre\":\"Córdoba\",\"simbolo\":\"C$\",\"activo\":true}";
        Stream<String> codigos = Stream.of("null", "\"\"", "\"NI\"", "\"NIOS\"", "\"123\"", "\"NÍO\"")
            .map(base::formatted);
        Stream<String> campos = Stream.of(
            "{\"codigo\":\"NIO\",\"nombre\":\" \",\"simbolo\":\"C$\",\"activo\":true}",
            "{\"codigo\":\"NIO\",\"nombre\":\"Córdoba\",\"simbolo\":\" \",\"activo\":true}",
            "{\"codigo\":\"NIO\",\"nombre\":\"Córdoba\",\"simbolo\":\"C$\"}",
            "{\"codigo\":\"NIO\",\"nombre\":\"" + "a".repeat(51) + "\",\"simbolo\":\"C$\",\"activo\":true}",
            "{\"codigo\":\"NIO\",\"nombre\":\"Córdoba\",\"simbolo\":\"123456\",\"activo\":true}");
        return Stream.concat(codigos, campos);
    }

    @Test
    void consultaPorId() throws Exception {
        when(service.obtenerPorId(1L)).thenReturn(respuesta());
        mvc.perform(get("/api/monedas/1")).andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value("NIO"));
    }

    @Test
    void actualizaConPut() throws Exception {
        when(service.actualizar(eq(1L), any(MonedaRequest.class))).thenReturn(respuesta());
        mvc.perform(put("/api/monedas/1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigo\":\"NIO\",\"nombre\":\"Córdoba\",\"simbolo\":\"C$\",\"activo\":true}"))
            .andExpect(status().isOk());
        verify(service).actualizar(eq(1L), any(MonedaRequest.class));
    }

    @Test
    void cambiaEstadoConPatch() throws Exception {
        when(service.cambiarEstado(1L, true)).thenReturn(respuesta());
        mvc.perform(patch("/api/monedas/1/estado").contentType(MediaType.APPLICATION_JSON).content("{\"activo\":true}"))
            .andExpect(status().isOk());
        verify(service).cambiarEstado(1L, true);
    }

    @Test
    void exigeEstadoExplicito() throws Exception {
        mvc.perform(patch("/api/monedas/1/estado").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void deleteSoloDesactivaYDevuelve204() throws Exception {
        mvc.perform(delete("/api/monedas/1")).andExpect(status().isNoContent());
        verify(service).cambiarEstado(1L, false);
    }

    @Test
    void filtraMonedasActivas() throws Exception {
        when(service.listar(eq(true), any(Pageable.class))).thenAnswer(inv -> Page.empty(inv.getArgument(1)));
        mvc.perform(get("/api/monedas?activo=true")).andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(20));
        verify(service).listar(eq(true), any(Pageable.class));
    }

    @Test
    void rechazaIdsInvalidos() throws Exception {
        mvc.perform(get("/api/monedas/0")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/monedas/-1")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void informaNoEncontrada() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe."));
        mvc.perform(get("/api/monedas/99")).andExpect(status().isNotFound());
    }

    @Test
    void informaCodigoDuplicado() throws Exception {
        when(service.crear(any())).thenThrow(new ConflictoException("Código duplicado."));
        mvc.perform(post("/api/monedas").contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigo\":\"NIO\",\"nombre\":\"Córdoba\",\"simbolo\":\"C$\",\"activo\":true}"))
            .andExpect(status().isConflict());
    }
}
