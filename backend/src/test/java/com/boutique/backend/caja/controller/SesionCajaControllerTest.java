package com.boutique.backend.caja.controller;

import java.math.BigDecimal;
import java.time.Instant;

import com.boutique.backend.caja.dto.SesionCajaResponse;
import com.boutique.backend.caja.entity.EstadoCaja;
import com.boutique.backend.caja.service.SesionCajaService;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

// Contrato HTTP aislado: no inicia el backend ni conexiones a datos.
@ExtendWith(MockitoExtension.class)
class SesionCajaControllerTest {

    @Mock SesionCajaService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void preparar() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new SesionCajaController(service)).setValidator(validator)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @AfterEach
    void cerrar() {
        validator.close();
    }

    private SesionCajaResponse respuesta() {
        return new SesionCajaResponse(10L, 2L, 7L, null, EstadoCaja.ABIERTA, Instant.now(), null,
            new BigDecimal("100.00"), null, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, 0, null);
    }

    @Test
    void abreCon201YUbicacion() throws Exception {
        when(service.abrir(any())).thenReturn(respuesta());
        mvc.perform(post("/api/cajas").contentType(MediaType.APPLICATION_JSON).content("{\"efectivoInicial\":100}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/cajas/10"))
            .andExpect(jsonPath("$.estado").value("ABIERTA")).andExpect(jsonPath("$.abiertoPorId").value(7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "-0.01", "0.001", "1000000000000.00", "\"texto\""})
    void rechazaMontosInvalidosEnAperturaYCierre(String monto) throws Exception {
        mvc.perform(post("/api/cajas").contentType(MediaType.APPLICATION_JSON)
            .content("{\"efectivoInicial\":" + monto + "}"))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/cajas/10/cierre").contentType(MediaType.APPLICATION_JSON)
            .content("{\"efectivoCierre\":" + monto + "}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void exigeMontoEnAmbasOperaciones() throws Exception {
        mvc.perform(post("/api/cajas").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/cajas/10/cierre").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void admiteElMaximoDeLaColumnaYElCero() throws Exception {
        when(service.abrir(any())).thenReturn(respuesta());
        when(service.cerrar(eq(10L), any())).thenReturn(respuesta());
        mvc.perform(post("/api/cajas").contentType(MediaType.APPLICATION_JSON)
            .content("{\"efectivoInicial\":999999999999.99}"))
            .andExpect(status().isCreated());
        mvc.perform(patch("/api/cajas/10/cierre").contentType(MediaType.APPLICATION_JSON)
            .content("{\"efectivoCierre\":0}"))
            .andExpect(status().isOk());
    }

    @Test
    void limitaNotasEnAperturaYCierre() throws Exception {
        String notas = "a".repeat(5001);
        mvc.perform(post("/api/cajas").contentType(MediaType.APPLICATION_JSON)
            .content("{\"efectivoInicial\":0,\"notas\":\"" + notas + "\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/cajas/10/cierre").contentType(MediaType.APPLICATION_JSON)
            .content("{\"efectivoCierre\":0,\"notas\":\"" + notas + "\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void consultaActualSinConfundirlaConId() throws Exception {
        when(service.obtenerActual()).thenReturn(respuesta());
        mvc.perform(get("/api/cajas/actual")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10));
        verify(service).obtenerActual();
        verify(service, never()).obtenerPorId(any());
    }

    @Test
    void informaQueNoHayCajaAbierta() throws Exception {
        when(service.obtenerActual()).thenThrow(new RecursoNoEncontradoException("No tienes una caja abierta."));
        mvc.perform(get("/api/cajas/actual")).andExpect(status().isNotFound());
    }

    @Test
    void consultaHistorialPorId() throws Exception {
        when(service.obtenerPorId(10L)).thenReturn(respuesta());
        mvc.perform(get("/api/cajas/10")).andExpect(status().isOk()).andExpect(jsonPath("$.sucursalId").value(2));
    }

    @Test
    void filtraHistorialPorSucursalUsuarioYEstado() throws Exception {
        when(service.listar(eq(2L), eq(7L), eq(EstadoCaja.CERRADA), any(Pageable.class)))
            .thenAnswer(inv -> Page.empty(inv.getArgument(3)));
        mvc.perform(get("/api/cajas?sucursalId=2&usuarioId=7&estado=CERRADA"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(20));
        verify(service).listar(eq(2L), eq(7L), eq(EstadoCaja.CERRADA), any(Pageable.class));
    }

    @Test
    void rechazaFiltrosEIdentificadoresInvalidos() throws Exception {
        mvc.perform(get("/api/cajas?sucursalId=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cajas?usuarioId=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cajas?estado=OTRO")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cajas/0")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/cajas/0/cierre").contentType(MediaType.APPLICATION_JSON)
            .content("{\"efectivoCierre\":0}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void informaConflictoAlCerrarDeNuevo() throws Exception {
        when(service.cerrar(eq(10L), any())).thenThrow(new ConflictoException("La caja ya está cerrada."));
        mvc.perform(patch("/api/cajas/10/cierre").contentType(MediaType.APPLICATION_JSON)
            .content("{\"efectivoCierre\":100}"))
            .andExpect(status().isConflict());
    }

    @Test
    void noExponeBorradoNiEdicionGeneralDeLaCaja() throws Exception {
        mvc.perform(delete("/api/cajas/10")).andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/cajas/10").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }
}
