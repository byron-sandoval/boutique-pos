package com.boutique.backend.venta.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.venta.dto.VentaResponse;
import com.boutique.backend.venta.entity.EstadoVenta;
import com.boutique.backend.venta.entity.MetodoPago;
import com.boutique.backend.venta.service.VentaService;
import com.boutique.backend.venta.service.VentaAnulacionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

@ExtendWith(MockitoExtension.class)
class VentaControllerTest {
    private static final String VALIDA = """
        {"sesionCajaId":5,"monedaId":1,"serieFacturaId":3,"metodoPago":"EFECTIVO",
         "tipoCambioAplicado":1.0000,"importeRecibido":250.00,
         "detalles":[{"varianteId":4,"cantidad":2,"descuento":20.00}]}
        """;

    @Mock VentaService service;
    @Mock VentaAnulacionService anulacionService;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void preparar() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new VentaController(service, anulacionService)).setValidator(validator)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @AfterEach
    void cerrar() {
        validator.close();
    }

    private VentaResponse respuesta() {
        return new VentaResponse(10L, 2L, 7L, null, 1L, 3L, 5L, 8L, "FAC-000008", Instant.now(),
            new BigDecimal("200.00"), new BigDecimal("20.00"), new BigDecimal("27.00"), new BigDecimal("207.00"),
            MetodoPago.EFECTIVO, new BigDecimal("1.0000"), new BigDecimal("250.00"), new BigDecimal("43.00"),
            EstadoVenta.COMPLETADA, null, null, null, List.of());
    }

    @Test
    void creaConDetallesYDevuelveNumeroYUbicacion() throws Exception {
        when(service.crear(any())).thenReturn(respuesta());
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(VALIDA))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/ventas/10"))
            .andExpect(jsonPath("$.numeroFactura").value("FAC-000008"))
            .andExpect(jsonPath("$.total").value(207.00));
    }

    @ParameterizedTest
    @MethodSource("solicitudesInvalidas")
    void validaAntesDeLlamarAlServicio(String body) throws Exception {
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    static Stream<String> solicitudesInvalidas() {
        return Stream.of(
            VALIDA.replace("\"sesionCajaId\":5", "\"sesionCajaId\":0"),
            VALIDA.replace("\"monedaId\":1", "\"monedaId\":null"),
            VALIDA.replace("\"serieFacturaId\":3", "\"serieFacturaId\":0"),
            VALIDA.replace("\"EFECTIVO\"", "\"CREDITO\""),
            VALIDA.replace("\"EFECTIVO\"", "null"),
            VALIDA.replace("\"tipoCambioAplicado\":1.0000", "\"tipoCambioAplicado\":0"),
            VALIDA.replace("\"tipoCambioAplicado\":1.0000", "\"tipoCambioAplicado\":0.00001"),
            VALIDA.replace("\"tipoCambioAplicado\":1.0000", "\"tipoCambioAplicado\":100000000"),
            VALIDA.replace("\"importeRecibido\":250.00", "\"importeRecibido\":-1"),
            VALIDA.replace("\"cantidad\":2", "\"cantidad\":0"),
            VALIDA.replace("\"varianteId\":4", "\"varianteId\":null"),
            VALIDA.replace("\"descuento\":20.00", "\"descuento\":-1"),
            VALIDA.replace("\"descuento\":20.00", "\"descuento\":0.001"),
            VALIDA.replace("[{\"varianteId\":4,\"cantidad\":2,\"descuento\":20.00}]", "[]"),
            VALIDA.replace("[{\"varianteId\":4,\"cantidad\":2,\"descuento\":20.00}]", "[null]"));
    }

    @Test
    void consultaVentaConDetalles() throws Exception {
        when(service.obtenerPorId(10L)).thenReturn(respuesta());
        mvc.perform(get("/api/ventas/10")).andExpect(status().isOk())
            .andExpect(jsonPath("$.detalles").isArray()).andExpect(jsonPath("$.estado").value("COMPLETADA"));
    }

    @Test
    void filtraPorSucursalCajaYEstado() throws Exception {
        when(service.listar(eq(2L), eq(5L), eq(EstadoVenta.COMPLETADA), any(Pageable.class)))
            .thenAnswer(inv -> Page.empty(inv.getArgument(3)));
        mvc.perform(get("/api/ventas?sucursalId=2&sesionCajaId=5&estado=COMPLETADA"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(20));
        verify(service).listar(eq(2L), eq(5L), eq(EstadoVenta.COMPLETADA), any(Pageable.class));
    }

    @Test
    void comunicaConflictosDelRegistro() throws Exception {
        when(service.crear(any())).thenThrow(new ConflictoException("Stock insuficiente."));
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(VALIDA))
            .andExpect(status().isConflict());
    }

    @Test
    void comunicaVentaInexistente() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe."));
        mvc.perform(get("/api/ventas/99")).andExpect(status().isNotFound());
    }

    @Test
    void validaIdentificadoresYFiltros() throws Exception {
        mvc.perform(get("/api/ventas/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ventas?sesionCajaId=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ventas?estado=OTRO")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void noExponeEdicionDeUnaVentaConfirmada() throws Exception {
        mvc.perform(put("/api/ventas/10").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }

    @Test
    void anulaConMotivoYDevuelveFacturaConAuditoria() throws Exception {
        VentaResponse anulada = new VentaResponse(10L, 2L, 7L, null, 1L, 3L, 5L, 8L, "FAC-000008", Instant.now(),
            new BigDecimal("200.00"), new BigDecimal("20.00"), new BigDecimal("27.00"), new BigDecimal("207.00"),
            MetodoPago.EFECTIVO, new BigDecimal("1.0000"), new BigDecimal("250.00"), new BigDecimal("43.00"),
            EstadoVenta.ANULADA, "Error de facturación", Instant.now(), 7L, List.of());
        when(anulacionService.anular(eq(10L), any())).thenReturn(anulada);
        mvc.perform(patch("/api/ventas/10/anulacion").contentType(MediaType.APPLICATION_JSON)
            .content("{\"motivo\":\"Error de facturación\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ANULADA"))
            .andExpect(jsonPath("$.numeroFactura").value("FAC-000008"))
            .andExpect(jsonPath("$.anuladaPorId").value(7));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"motivo\":null}", "{\"motivo\":\"\"}", "{\"motivo\":\"   \"}"})
    void exigeMotivoDeAnulacion(String body) throws Exception {
        mvc.perform(patch("/api/ventas/10/anulacion").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(anulacionService, service);
    }

    @Test
    void limitaLongitudDelMotivo() throws Exception {
        mvc.perform(patch("/api/ventas/10/anulacion").contentType(MediaType.APPLICATION_JSON)
            .content("{\"motivo\":\"" + "a".repeat(256) + "\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(anulacionService);
    }

    @Test
    void validaIdAlAnular() throws Exception {
        mvc.perform(patch("/api/ventas/0/anulacion").contentType(MediaType.APPLICATION_JSON)
            .content("{\"motivo\":\"Error\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(anulacionService);
    }

    @Test
    void comunicaConflictosDeAnulacion() throws Exception {
        when(anulacionService.anular(eq(10L), any())).thenThrow(new ConflictoException("Caja cerrada."));
        mvc.perform(patch("/api/ventas/10/anulacion").contentType(MediaType.APPLICATION_JSON)
            .content("{\"motivo\":\"Error\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void comunicaVentaInexistenteAlAnular() throws Exception {
        when(anulacionService.anular(eq(99L), any())).thenThrow(new RecursoNoEncontradoException("No existe."));
        mvc.perform(patch("/api/ventas/99/anulacion").contentType(MediaType.APPLICATION_JSON)
            .content("{\"motivo\":\"Error\"}"))
            .andExpect(status().isNotFound());
    }
}
