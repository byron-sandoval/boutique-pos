package com.boutique.backend.usuario.keycloak;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del cliente administrativo de Keycloak.
 *
 * @param serverUrl direccion del servidor Keycloak
 * @param realm realm que contiene las cuentas del POS
 * @param clientId cliente de servicio autorizado para administrar cuentas
 * @param clientSecret secreto del cliente, proporcionado mediante una variable de entorno
 */
@ConfigurationProperties(prefix = "app.keycloak.admin")
public record KeycloakAdminProperties(URI serverUrl, String realm, String clientId, String clientSecret) {

    @Override
    public String toString() {
        return "KeycloakAdminProperties[credenciales reservadas]";
    }
}
