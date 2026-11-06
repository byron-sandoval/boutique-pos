package com.boutique.backend.usuario.keycloak;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.ServicioExternoException;
import com.boutique.backend.usuario.dto.UsuarioCreateRequest;
import com.boutique.backend.usuario.dto.UsuarioResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class KeycloakAdminClient {

    private final KeycloakAdminProperties properties;
    private final RestClient client;

    public KeycloakAdminClient(KeycloakAdminProperties properties,
        @Qualifier("keycloakAdminRestClient") RestClient client) {
        this.properties = properties;
        this.client = client;
    }

    public String crearDeshabilitado(UsuarioCreateRequest request) {
        return ejecutar(() -> {
            String token = token();
            Map<String, Object> perfil = Map.of(
                "username", request.username(), "firstName", request.nombre(),
                "lastName", valor(request.apellido()), "email", valor(request.email()),
                "enabled", false, "emailVerified", false,
                "credentials", List.of(Map.of("type", "password", "value", request.password(), "temporary", true))
            );
            ResponseEntity<Void> response = client.post().uri("/admin/realms/{realm}/users", properties.realm())
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON).body(perfil).retrieve().toBodilessEntity();
            URI location = response.getHeaders().getLocation();
            if (response.getStatusCode().value() != 201 || location == null || location.getPath() == null) {
                throw new ServicioExternoException("Keycloak no confirmo la cuenta creada. Revisa el usuario antes de reintentar.");
            }
            String id = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
            if (id.isBlank() || id.length() > 100) {
                throw new ServicioExternoException("Keycloak devolvio un identificador no valido.");
            }
            return id;
        });
    }

    public void asignarRolInicial(String id, String rol) {
        ejecutar(() -> {
            String token = token();
            Map<String, Object> role = client.get().uri("/admin/realms/{realm}/roles/{role}", properties.realm(), rol)
                .headers(headers -> headers.setBearerAuth(token)).retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (role == null || !rol.equals(role.get("name"))) {
                throw new ServicioExternoException("El rol del POS no esta configurado en Keycloak.");
            }
            client.post().uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", properties.realm(), id)
                .headers(headers -> headers.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role)).retrieve().toBodilessEntity();
            return null;
        });
    }

    public void actualizarPerfil(UsuarioResponse usuario) {
        ejecutar(() -> {
            String token = token();
            client.put().uri("/admin/realms/{realm}/users/{id}", properties.realm(), usuario.keycloakId())
                .headers(headers -> headers.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("firstName", usuario.nombre(), "lastName", valor(usuario.apellido()),
                    "email", valor(usuario.email()), "emailVerified", false))
                .retrieve().toBodilessEntity();
            return null;
        });
    }

    public void cambiarEstado(String id, boolean activo) {
        ejecutar(() -> {
            String token = token();
            client.put().uri("/admin/realms/{realm}/users/{id}", properties.realm(), id)
                .headers(headers -> headers.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", activo)).retrieve().toBodilessEntity();
            if (!activo) {
                client.post().uri("/admin/realms/{realm}/users/{id}/logout", properties.realm(), id)
                    .headers(headers -> headers.setBearerAuth(token)).retrieve().toBodilessEntity();
            }
            return null;
        });
    }

    // Solo compensa cuentas recien creadas cuya transaccion local se cancelo.
    public void eliminarCuentaNoPersistida(String id) {
        ejecutar(() -> {
            String token = token();
            client.delete().uri("/admin/realms/{realm}/users/{id}", properties.realm(), id)
                .headers(headers -> headers.setBearerAuth(token)).retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {})
                .toBodilessEntity();
            return null;
        });
    }

    private String token() {
        if (properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new ServicioExternoException("Falta configurar KEYCLOAK_ADMIN_CLIENT_SECRET para administrar usuarios.");
        }
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        TokenResponse response = client.post().uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(TokenResponse.class);
        if (response == null || response.access_token() == null || response.access_token().isBlank()) {
            throw new ServicioExternoException("Keycloak no devolvio un token de servicio valido.");
        }
        return response.access_token();
    }

    private String valor(String value) {
        return value == null ? "" : value;
    }

    private <T> T ejecutar(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 400) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Keycloak rechazo los datos del usuario o su politica de contrasenas.");
            }
            if (exception.getStatusCode().value() == 409) {
                throw new ConflictoException("Keycloak detecto un usuario o correo duplicado.");
            }
            throw new ServicioExternoException("Keycloak rechazo la operacion. Revisa su configuracion y los datos del usuario.");
        } catch (RestClientException exception) {
            throw new ServicioExternoException("No se pudo completar la operacion con Keycloak.");
        }
    }

    private record TokenResponse(String access_token) {
        @Override
        public String toString() {
            return "TokenResponse[token reservado]";
        }
    }
}
