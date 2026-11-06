package com.boutique.backend.inventario.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import com.boutique.backend.inventario.dto.*;
import com.boutique.backend.inventario.service.IngresoService;
import com.boutique.backend.inventario.service.InventarioService;
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
class InventarioIngresoControllerTest {
    @Mock InventarioService inventarios;
    @Mock IngresoService ingresos;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void preparar() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new InventarioController(inventarios), new IngresoController(ingresos))
            .setValidator(validator).setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @AfterEach
    void cerrar() {
        validator.close();
    }

    @Test
    void creaIngresoConDetallesYDevuelveUbicacion() throws Exception {
        when(ingresos.crear(any())).thenReturn(new IngresoResponse(10L, 1L, null, 8L, Instant.now(), "F1",
            new BigDecimal("2.00"), null, true, List.of()));
        mvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON).content("""
            {"sucursalId":1,"noDocumento":"F1","detalles":[{"varianteId":4,"cantidad":2,"costoUnitario":1.00}]}
            """))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/ingresos/10"))
            .andExpect(jsonPath("$.usuarioId").value(8));
    }

    @ParameterizedTest
    @MethodSource("ingresosInvalidos")
    void validaDetallesAntesDeLlamarServicio(String body) throws Exception {
        mvc.perform(post("/api/ingresos").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(ingresos);
    }

    static Stream<String> ingresosInvalidos() {
        String base = "{\"sucursalId\":1,\"noDocumento\":\"F1\",\"detalles\":%s}";
        return Stream.of("[]", "[null]", "[{\"varianteId\":4,\"cantidad\":0,\"costoUnitario\":1}]",
            "[{\"varianteId\":4,\"cantidad\":1,\"costoUnitario\":-1}]",
            "[{\"varianteId\":4,\"cantidad\":1,\"costoUnitario\":0.001}]",
            "[{\"varianteId\":4,\"cantidad\":1,\"costoUnitario\":10000000000}]",
            "[{\"varianteId\":4,\"cantidad\":1}]", "[{\"cantidad\":1,\"costoUnitario\":1}]")
            .map(base::formatted);
    }

    @Test
    void inventarioSeCreaEnCero() throws Exception {
        when(inventarios.crear(any())).thenReturn(new InventarioResponse(1L, 4L, 1L, 0, 2, null));
        mvc.perform(post("/api/inventarios").contentType(MediaType.APPLICATION_JSON)
            .content("{\"varianteId\":4,\"sucursalId\":1,\"stockMinimo\":2}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.stock").value(0));
    }

    @Test
    void rechazaLimitesInvertidos() throws Exception {
        mvc.perform(put("/api/inventarios/1/limites").contentType(MediaType.APPLICATION_JSON)
            .content("{\"stockMinimo\":5,\"stockMaximo\":2}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(inventarios);
    }

    @Test
    void rechazaLimitesNegativos() throws Exception {
        mvc.perform(post("/api/inventarios").contentType(MediaType.APPLICATION_JSON)
            .content("{\"varianteId\":4,\"sucursalId\":1,\"stockMinimo\":-1}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(inventarios);
    }

    @Test
    void filtraExistenciasPorSucursalYVariante() throws Exception {
        when(inventarios.listar(eq(1L), eq(4L), any(Pageable.class)))
            .thenAnswer(invocation -> Page.empty(invocation.getArgument(2)));
        mvc.perform(get("/api/inventarios?sucursalId=1&varianteId=4"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(20));
        verify(inventarios).listar(eq(1L), eq(4L), any(Pageable.class));
    }

    @Test
    void rechazaIdentificadoresInvalidos() throws Exception {
        mvc.perform(get("/api/inventarios?sucursalId=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ingresos/0")).andExpect(status().isBadRequest());
        verifyNoInteractions(inventarios, ingresos);
    }

    @Test
    void deleteAnulaIngreso() throws Exception {
        mvc.perform(delete("/api/ingresos/10")).andExpect(status().isNoContent());
        verify(ingresos).anular(10L);
    }

    @Test
    void noExponeBorradoDeExistenciasNiEdicionDeIngresoConfirmado() throws Exception {
        mvc.perform(delete("/api/inventarios/1")).andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/ingresos/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(inventarios, ingresos);
    }
}
