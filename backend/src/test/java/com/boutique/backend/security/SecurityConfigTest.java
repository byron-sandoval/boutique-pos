package com.boutique.backend.security;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

import com.boutique.backend.empresa.controller.EmpresaController;
import com.boutique.backend.empresa.controller.SucursalController;
import com.boutique.backend.empresa.dto.EmpresaResponse;
import com.boutique.backend.empresa.dto.SucursalResponse;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.service.EmpresaService;
import com.boutique.backend.empresa.service.SucursalService;
import com.boutique.backend.usuario.controller.UsuarioController;
import com.boutique.backend.usuario.dto.UsuarioResponse;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import com.boutique.backend.usuario.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.web.MockServletContext;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Contexto MVC minimo: decoder y repositorio simulados, sin Spring Boot ni red.
class SecurityConfigTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private UsuarioRepository usuarios;
    private JwtDecoder decoder;
    private UsuarioService usuarioService;
    private EmpresaService empresaService;
    private SucursalService sucursalService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.getEnvironment().setActiveProfiles("usuarios-security-test");
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class, SecurityConfig.class);
        context.refresh();
        usuarios = context.getBean(UsuarioRepository.class);
        decoder = context.getBean(JwtDecoder.class);
        usuarioService = context.getBean(UsuarioService.class);
        empresaService = context.getBean(EmpresaService.class);
        sucursalService = context.getBean(SucursalService.class);
        when(decoder.decode("admin-token")).thenReturn(token("ADMIN"));
        when(decoder.decode("cajero-token")).thenReturn(token("CAJERO"));
        when(usuarioService.listar(any())).thenReturn(Page.empty());
        when(usuarioService.obtenerPorId(5L)).thenReturn(response());
        when(empresaService.listar(any())).thenReturn(Page.empty());
        when(empresaService.obtenerPorId(42L)).thenReturn(new EmpresaResponse(
            42L, "Boutique", null, null, "Centro", null, null, null, null, new BigDecimal("15.00")));
        when(sucursalService.listar(any())).thenReturn(Page.empty());
        when(sucursalService.obtenerPorId(84L)).thenReturn(new SucursalResponse(84L, 42L, "Principal", "Centro", null, false));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void usuariosRequierenAutenticacionBearer() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void cajeroNoPuedeConsultarUsuariosPorLaAnotacionDeClase() throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, true)));

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/5").header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void cajeroNoPuedeModificarUsuarios(String metodo) throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, true)));

        mvc.perform(escritura(metodo).header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void adminPuedeModificarUsuarios(String metodo) throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, true)));
        when(usuarioService.crear(any())).thenReturn(response());
        when(usuarioService.actualizar(anyLong(), any())).thenReturn(response());
        when(usuarioService.cambiarEstado(anyLong(), anyBoolean())).thenReturn(response());
        int status = metodo.equals("POST") ? 201 : metodo.equals("DELETE") ? 204 : 200;

        mvc.perform(escritura(metodo).header("Authorization", "Bearer admin-token"))
            .andExpect(status().is(status));
    }

    @Test
    void adminActivoPuedeAdministrarUsuarios() throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, true)));

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk());
    }

    @Test
    void adminDesactivadoNoPuedeUsarUnTokenAnterior() throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(false, true)));

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    void sucursalDesactivadaImpideElAcceso() throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, false)));

        mvc.perform(get("/api/prueba").header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/empresas", "/api/empresas/42", "/api/sucursales", "/api/sucursales/84"})
    void adminActivoPuedeConsultarConfiguracionConSuSucursalInactiva(String ruta) throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, false)));

        mvc.perform(get(ruta).header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk());
    }

    @Test
    void adminPuedeDesactivarSuSucursalConsultarYReactivarlaSinBloquearse() throws Exception {
        Usuario admin = usuario(true, true);
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(admin));
        when(sucursalService.cambiarEstado(anyLong(), anyBoolean())).thenAnswer(invocation -> {
            boolean activa = invocation.getArgument(1);
            admin.getSucursal().setActiva(activa);
            return new SucursalResponse(84L, 42L, "Principal", "Centro", null, activa);
        });

        mvc.perform(patch("/api/sucursales/84/estado").header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":false}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/sucursales").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/prueba").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/sucursales/84/estado").header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":true}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/prueba").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void administradorDesactivadoNoObtieneLaExcepcionDeConfiguracion(boolean sucursalActiva) throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(false, sucursalActiva)));

        mvc.perform(get("/api/empresas").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/sucursales").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/sucursales/84/estado").header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":true}"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(empresaService, sucursalService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cajeroNoObtieneLaExcepcionDeConfiguracion(boolean sucursalActiva) throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, sucursalActiva)));

        mvc.perform(get("/api/empresas").header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/sucursales").header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/sucursales/84/estado").header("Authorization", "Bearer cajero-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":true}"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(empresaService, sucursalService);
    }

    @Test
    void nombreDeEmpresaSePuedeConsultarDesdeElMenuSegunElAccesoDelUsuario() throws Exception {
        when(empresaService.nombreConfigurado()).thenReturn("Boutique David");
        mvc.perform(get("/api/empresas/nombre")).andExpect(status().isUnauthorized());

        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, true)));
        mvc.perform(get("/api/empresas/nombre").header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Boutique David"))
            .andExpect(jsonPath("$.ruc").doesNotExist());
        mvc.perform(get("/api/empresas/42").header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isForbidden());

        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, false)));
        mvc.perform(get("/api/empresas/nombre").header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/empresas/nombre").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk());
    }

    @Test
    void adminSinSucursalActivaSigueBloqueadoEnOtrasRutas() throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, false)));

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/ventas").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/cajas").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    void configuracionRequiereTokenPeroPermiteElAdminInicial() throws Exception {
        mvc.perform(get("/api/sucursales")).andExpect(status().isUnauthorized());
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.empty());

        mvc.perform(get("/api/empresas").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/sucursales").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk());
    }

    @Test
    void permitePrepararElPosConUnAdminInicialSinRegistroLocal() throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.empty());

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk());
    }

    @Test
    void cajeroSinVinculoLocalNoTieneAcceso() throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.empty());

        mvc.perform(get("/api/prueba").header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    void cajeroActivoPuedeUsarOtrasRutasDelPos() throws Exception {
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.of(usuario(true, true)));

        mvc.perform(get("/api/prueba").header("Authorization", "Bearer cajero-token"))
            .andExpect(status().isOk());
    }

    @Test
    void tokenInvalidoNoSeAcepta() throws Exception {
        when(decoder.decode("invalido")).thenThrow(new BadJwtException("invalido"));

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer invalido"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void claimsDeRolMalformadosNoOtorganPermisos() throws Exception {
        Jwt jwt = Jwt.withTokenValue("malformado").header("alg", "RS256").subject("kc-1")
            .claim("realm_access", "ADMIN").build();
        when(decoder.decode("malformado")).thenReturn(jwt);

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer malformado"))
            .andExpect(status().isForbidden());
    }

    @Test
    void preflightDelFrontendNoNecesitaToken() throws Exception {
        mvc.perform(options("/api/usuarios").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void preflightDeOtroOrigenEsRechazado() throws Exception {
        mvc.perform(options("/api/usuarios").header("Origin", "https://otro.example")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden());
    }

    @Test
    void unJwtSinSubjectNoObtieneAccesoDeBootstrap() {
        Jwt jwt = Jwt.withTokenValue("sin-sub").header("alg", "RS256")
            .claim("realm_access", Map.of("roles", List.of("ADMIN"))).build();

        assertFalse(context.getBean(UsuarioAccessService.class)
            .puedeAcceder(new KeycloakJwtConverter().convert(jwt)));
        assertFalse(context.getBean(UsuarioAccessService.class)
            .puedeAdministrarEmpresa(new KeycloakJwtConverter().convert(jwt)));
    }

    private Jwt token(String rol) {
        return Jwt.withTokenValue(rol).header("alg", "RS256").subject("kc-1")
            .claim("realm_access", Map.of("roles", List.of(rol))).build();
    }

    private UsuarioResponse response() {
        return new UsuarioResponse(5L, "kc-1", 1L, "CED-1", "Operador", null, null, null, "operador", true);
    }

    private MockHttpServletRequestBuilder escritura(String metodo) {
        return switch (metodo) {
            case "POST" -> post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"sucursalId":1,"cedula":"CED-1","nombre":"Operador","username":"operador",
                 "rol":"CAJERO","activo":true,"password":"Temporal-prueba-123"}
                """);
            case "PUT" -> put("/api/usuarios/5").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sucursalId\":1,\"cedula\":\"CED-1\",\"nombre\":\"Operador\"}");
            case "PATCH" -> patch("/api/usuarios/5/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}");
            case "DELETE" -> delete("/api/usuarios/5");
            default -> throw new IllegalArgumentException(metodo);
        };
    }

    private Usuario usuario(boolean activo, boolean sucursalActiva) {
        Sucursal sucursal = new Sucursal();
        sucursal.setActiva(sucursalActiva);
        Usuario usuario = new Usuario();
        usuario.setActivo(activo);
        usuario.setSucursal(sucursal);
        return usuario;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableWebMvc
    static class TestConfig {
        @Bean UsuarioRepository usuarioRepository() { return mock(UsuarioRepository.class); }
        @Bean JwtDecoder jwtDecoder() { return mock(JwtDecoder.class); }
        @Bean KeycloakJwtConverter converter() { return new KeycloakJwtConverter(); }
        @Bean UsuarioAccessService access(UsuarioRepository repository) { return new UsuarioAccessService(repository); }
        @Bean UsuarioService usuarioService() { return mock(UsuarioService.class); }
        @Bean UsuarioController usuarioController(UsuarioService service) { return new UsuarioController(service); }
        @Bean EmpresaService empresaService() { return mock(EmpresaService.class); }
        @Bean SucursalService sucursalService() { return mock(SucursalService.class); }
        @Bean EmpresaController empresaController(EmpresaService service) { return new EmpresaController(service); }
        @Bean SucursalController sucursalController(SucursalService service) { return new SucursalController(service); }
        @Bean WebMvcConfigurer paginacion() {
            return new WebMvcConfigurer() {
                @Override
                public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                    resolvers.add(new PageableHandlerMethodArgumentResolver());
                }
            };
        }
        @Bean ProbeController probeController() { return new ProbeController(); }
    }

    @RestController
    @Profile("usuarios-security-test")
    static class ProbeController {
        @GetMapping("/api/prueba")
        public String consulta() { return "ok"; }
    }
}
