package com.boutique.backend.usuario.repository;

import java.util.Optional;

import com.boutique.backend.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCedula(String cedula);

    boolean existsByCedulaAndIdNot(String cedula, Long id);

    Optional<Usuario> findByKeycloakId(String keycloakId);
}
