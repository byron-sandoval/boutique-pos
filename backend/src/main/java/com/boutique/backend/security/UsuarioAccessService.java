package com.boutique.backend.security;

import com.boutique.backend.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioAccessService {

    private final UsuarioRepository usuarioRepository;

    public boolean puedeAcceder(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            return false;
        }
        if (token.getToken().getSubject() == null || token.getToken().getSubject().isBlank()) {
            return false;
        }
        boolean admin = token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean cajero = token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CAJERO"));
        if (!admin && !cajero) {
            return false;
        }
        return usuarioRepository.findByKeycloakId(token.getToken().getSubject())
            .map(usuario -> Boolean.TRUE.equals(usuario.getActivo())
                && Boolean.TRUE.equals(usuario.getSucursal().getActiva()))
            // Permite preparar el POS con un ADMIN inicial creado en Keycloak.
            .orElse(admin);
    }
}
