package com.boutique.backend.venta.controller;

import java.util.stream.Stream;

import com.boutique.backend.venta.dto.SerieFacturaResponse;
import com.boutique.backend.venta.service.SerieFacturaService;
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
class SerieFacturaControllerTest {
    @Mock SerieFacturaService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void preparar() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new SerieFacturaController(service)).setValidator(validator)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @AfterEach
    void cerrar() {
        validator.close();
    }

    private SerieFacturaResponse respuesta() {
        return new SerieFacturaResponse(1L, 2L, "FAC-", 6, 1L, true);
    }

    @Test
    void creaSerieYDevuelveUbicacion() throws Exception {
        when(service.crear(any())).thenReturn(respuesta());
        mvc.perform(post("/api/series-factura").contentType(MediaType.APPLICATION_JSON).content("""
            {"sucursalId":2,"prefijo":"fac-","digitos":6,"siguienteNumero":1,"activa":true}
            """))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/series-factura/1"))
            .andExpect(jsonPath("$.prefijo").value("FAC-"));
    }

    @ParameterizedTest
    @MethodSource("solicitudesInvalidas")
    void rechazaDatosInvalidosAlCrear(String body) throws Exception {
        mvc.perform(post("/api/series-factura").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    static Stream<String> solicitudesInvalidas() {
        String base = "{\"sucursalId\":2,\"prefijo\":\"FAC-\",\"digitos\":6,\"siguienteNumero\":1,\"activa\":true}";
        return Stream.of(
            base.replace("\"sucursalId\":2", "\"sucursalId\":0"),
            base.replace("\"sucursalId\":2", "\"sucursalId\":null"),
            base.replace("\"FAC-\"", "\"\""),
            base.replace("\"FAC-\"", "\"FAC /\""),
            base.replace("FAC-", "a".repeat(21)),
            base.replace("\"digitos\":6", "\"digitos\":0"),
            base.replace("\"digitos\":6", "\"digitos\":16"),
            base.replace("\"digitos\":6", "\"digitos\":null"),
            base.replace("\"siguienteNumero\":1", "\"siguienteNumero\":0"),
            base.replace("\"siguienteNumero\":1", "\"siguienteNumero\":null"),
            base.replace("\"siguienteNumero\":1", "\"siguienteNumero\":9223372036854775807"),
            base.replace("\"activa\":true", "\"activa\":null"));
    }

    @Test
    void actualizaFormatoSinPedirContadorNiSucursal() throws Exception {
        when(service.actualizar(eq(1L), any())).thenReturn(respuesta());
        mvc.perform(put("/api/series-factura/1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"prefijo\":\"FAC-\",\"digitos\":6,\"activa\":true}"))
            .andExpect(status().isOk());
        verify(service).actualizar(eq(1L), any());
    }

    @Test
    void validaFormatoAlActualizar() throws Exception {
        mvc.perform(put("/api/series-factura/1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"prefijo\":\"FAC-\",\"digitos\":16,\"activa\":true}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void consultaSerieSinConsumirNumero() throws Exception {
        when(service.obtenerPorId(1L)).thenReturn(respuesta());
        mvc.perform(get("/api/series-factura/1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.siguienteNumero").value(1));
        verify(service).obtenerPorId(1L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void filtraSeriesPorSucursalYEstado() throws Exception {
        when(service.listar(eq(2L), eq(true), any(Pageable.class))).thenAnswer(inv -> Page.empty(inv.getArgument(2)));
        mvc.perform(get("/api/series-factura?sucursalId=2&activa=true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(20));
        verify(service).listar(eq(2L), eq(true), any(Pageable.class));
    }

    @Test
    void cambiaEstadoConPatch() throws Exception {
        when(service.cambiarEstado(1L, true)).thenReturn(respuesta());
        mvc.perform(patch("/api/series-factura/1/estado").contentType(MediaType.APPLICATION_JSON)
            .content("{\"activa\":true}"))
            .andExpect(status().isOk());
        verify(service).cambiarEstado(1L, true);
    }

    @Test
    void exigeEstadoExplicito() throws Exception {
        mvc.perform(patch("/api/series-factura/1/estado").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void deleteDesactivaYDevuelve204() throws Exception {
        mvc.perform(delete("/api/series-factura/1")).andExpect(status().isNoContent());
        verify(service).cambiarEstado(1L, false);
    }

    @Test
    void rechazaIdentificadoresInvalidos() throws Exception {
        mvc.perform(get("/api/series-factura/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/series-factura?sucursalId=0")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/series-factura/-1")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
