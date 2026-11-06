package com.boutique.backend.usuario.keycloak;

import java.net.URI;

import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.ServicioExternoException;
import com.boutique.backend.usuario.dto.RolUsuario;
import com.boutique.backend.usuario.dto.UsuarioCreateRequest;
import com.boutique.backend.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// MockRestServiceServer intercepta todas las peticiones; no contacta Keycloak.
class KeycloakAdminClientTest {

    private MockRestServiceServer server;
    private KeycloakAdminClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://keycloak.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KeycloakAdminClient(properties("secreto-falso"), builder.build());
    }

    @Test
    void exigeElSecretoSinHacerPeticionesSiFalta() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(builder).build();
        KeycloakAdminClient unconfigured = new KeycloakAdminClient(properties(""), builder.build());

        assertThrows(ServicioExternoException.class, () -> unconfigured.crearDeshabilitado(request()));

        emptyServer.verify();
    }

    @Test
    void creaUnaCuentaDeshabilitadaConContrasenaTemporalYRecuperaSuId() {
        token();
        server.expect(requestTo("http://keycloak.test/admin/realms/boutique/users"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.enabled").value(false))
            .andExpect(jsonPath("$.credentials[0].temporary").value(true))
            .andExpect(jsonPath("$.credentials[0].value").value("Temporal-prueba-123"))
            .andRespond(withStatus(HttpStatus.CREATED).location(URI.create("http://keycloak.test/admin/realms/boutique/users/kc-1")));

        assertEquals("kc-1", client.crearDeshabilitado(request()));

        server.verify();
    }

    @Test
    void traduceUsuarioDuplicadoSinExponerLaRespuestaDelServidor() {
        token();
        server.expect(requestTo("http://keycloak.test/admin/realms/boutique/users"))
            .andRespond(withStatus(HttpStatus.CONFLICT).body("dato-reservado-del-servidor"));

        ConflictoException error = assertThrows(ConflictoException.class, () -> client.crearDeshabilitado(request()));

        assertFalse(error.getMessage().contains("dato-reservado"));
        server.verify();
    }

    @Test
    void erroresDelTokenNoExponenCredencialesNiCuerposRemotos() {
        server.expect(requestTo("http://keycloak.test/realms/boutique/protocol/openid-connect/token"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("secreto-del-servidor"));

        ServicioExternoException error = assertThrows(ServicioExternoException.class,
            () -> client.crearDeshabilitado(request()));

        assertFalse(error.getMessage().contains("secreto-del-servidor"));
        server.verify();
    }

    @Test
    void asignaElRolDelRealmSinUsarCredencialesDelAdministradorHumano() {
        token();
        server.expect(requestTo("http://keycloak.test/admin/realms/boutique/roles/CAJERO"))
            .andRespond(withSuccess("{\"id\":\"rol-1\",\"name\":\"CAJERO\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak.test/admin/realms/boutique/users/kc-1/role-mappings/realm"))
            .andExpect(method(HttpMethod.POST)).andExpect(jsonPath("$[0].name").value("CAJERO"))
            .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.asignarRolInicial("kc-1", "CAJERO");

        server.verify();
    }

    @Test
    void desactivarDeshabilitaLaCuentaYCierraLasSesiones() {
        token();
        server.expect(requestTo("http://keycloak.test/admin/realms/boutique/users/kc-1"))
            .andExpect(method(HttpMethod.PUT)).andExpect(jsonPath("$.enabled").value(false))
            .andRespond(withStatus(HttpStatus.NO_CONTENT));
        server.expect(requestTo("http://keycloak.test/admin/realms/boutique/users/kc-1/logout"))
            .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.cambiarEstado("kc-1", false);

        server.verify();
    }

    @Test
    void compensaSolamenteLaCuentaIdentificada() {
        token();
        server.expect(requestTo("http://keycloak.test/admin/realms/boutique/users/kc-1"))
            .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.eliminarCuentaNoPersistida("kc-1");

        server.verify();
    }

    @Test
    void unaCuentaYaRetiradaNoHaceFallarLaCompensacion() {
        token();
        server.expect(requestTo("http://keycloak.test/admin/realms/boutique/users/kc-1"))
            .andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        client.eliminarCuentaNoPersistida("kc-1");

        server.verify();
    }

    @Test
    void actualizarPerfilNoCambiaUsernameContrasenaNiEstado() {
        token();
        server.expect(requestTo("http://keycloak.test/admin/realms/boutique/users/kc-1"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonPath("$.firstName").value("Nuevo nombre"))
            .andExpect(jsonPath("$.enabled").doesNotExist())
            .andExpect(jsonPath("$.username").doesNotExist())
            .andExpect(jsonPath("$.credentials").doesNotExist())
            .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.actualizarPerfil(new UsuarioResponse(1L, "kc-1", 2L, "CED-1", "Nuevo nombre",
            null, null, null, "operador", true));

        server.verify();
    }

    @Test
    void unaContrasenaRechazadaPorLaPoliticaSeInformaComo400() {
        token();
        server.expect(requestTo("http://keycloak.test/admin/realms/boutique/users"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("no-exponer-credenciales"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> client.crearDeshabilitado(request()));

        assertEquals(400, error.getStatusCode().value());
        assertFalse(error.getMessage().contains("no-exponer-credenciales"));
        server.verify();
    }

    private void token() {
        server.expect(requestTo("http://keycloak.test/realms/boutique/protocol/openid-connect/token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{\"access_token\":\"token-falso\"}", MediaType.APPLICATION_JSON));
    }

    private KeycloakAdminProperties properties(String secret) {
        return new KeycloakAdminProperties(URI.create("http://keycloak.test"), "boutique", "boutique-api", secret);
    }

    private UsuarioCreateRequest request() {
        return new UsuarioCreateRequest(1L, "CED-1", "Operador", null, null, null,
            "operador", RolUsuario.CAJERO, true, "Temporal-prueba-123");
    }
}
