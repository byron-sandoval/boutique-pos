package com.boutique.backend.usuario.controller;

import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.shared.exception.ServicioExternoException;
import com.boutique.backend.usuario.dto.UsuarioCreateRequest;
import com.boutique.backend.usuario.dto.UsuarioResponse;
import com.boutique.backend.usuario.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Validacion y respuestas HTTP sin servicios reales; permisos en SecurityConfigTest.
@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock private UsuarioService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new UsuarioController(service))
            .setControllerAdvice(new UsuarioErrorHandler()).setValidator(validator)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @AfterEach
    void tearDown() { validator.close(); }

    @Test
    void altaDevuelve201YNoExponeLaContrasena() throws Exception {
        when(service.crear(any(UsuarioCreateRequest.class))).thenReturn(response(true));

        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(body("CAJERO", "Temporal-prueba-123")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/usuarios/5"))
            .andExpect(jsonPath("$.keycloakId").value("kc-1"))
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void contrasenaInvalidaNoSeIncluyeEnLaRespuestaDeError() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(body("CAJERO", "XyZ789")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(not(containsString("XyZ789"))));

        verifyNoInteractions(service);
    }

    @Test
    void soloAceptaLosRolesDelPos() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(body("SUPERADMIN", "Temporal-prueba-123")))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void requiereUnaSucursalValida() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(body("CAJERO", "Temporal-prueba-123").replace("\"sucursalId\":1", "\"sucursalId\":0")))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void erroresDeIntegracionDevuelven503ConInstruccionDeRecuperacion() throws Exception {
        when(service.crear(any(UsuarioCreateRequest.class)))
            .thenThrow(new ServicioExternoException("Usuario guardado localmente; sincronizacion pendiente."));

        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(body("CAJERO", "Temporal-prueba-123")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.detail").value("Usuario guardado localmente; sincronizacion pendiente."));
    }

    @Test
    void usuarioInexistenteSeInformaComo404() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe el usuario."));

        mvc.perform(get("/api/usuarios/99")).andExpect(status().isNotFound());
    }

    @Test
    void exigeEstadoExplicito() throws Exception {
        mvc.perform(patch("/api/usuarios/5/estado").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deleteEsDesactivacionYDevuelve204() throws Exception {
        when(service.cambiarEstado(5L, false)).thenReturn(response(false));

        mvc.perform(delete("/api/usuarios/5")).andExpect(status().isNoContent());

        verify(service).cambiarEstado(5L, false);
    }

    private String body(String rol, String password) {
        return """
            {"sucursalId":1,"cedula":"CED-1","nombre":"Operador","username":"operador",
             "rol":"%s","activo":true,"password":"%s"}
            """.formatted(rol, password);
    }

    private UsuarioResponse response(boolean activo) {
        return new UsuarioResponse(5L, "kc-1", 1L, "CED-1", "Operador", null, null, null, "operador", activo);
    }
}
