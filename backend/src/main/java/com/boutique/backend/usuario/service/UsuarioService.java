package com.boutique.backend.usuario.service;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.shared.exception.ServicioExternoException;
import com.boutique.backend.usuario.dto.UsuarioCreateRequest;
import com.boutique.backend.usuario.dto.UsuarioResponse;
import com.boutique.backend.usuario.dto.UsuarioUpdateRequest;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.keycloak.KeycloakAdminClient;
import com.boutique.backend.usuario.mapper.UsuarioMapper;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioMapper usuarioMapper;
    private final KeycloakAdminClient keycloak;

    public Page<UsuarioResponse> listar(@NotNull Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(usuarioMapper::toResponse);
    }

    public UsuarioResponse obtenerPorId(@NotNull @Positive Long id) {
        return usuarioMapper.toResponse(buscarUsuario(id));
    }

    @Transactional
    public UsuarioResponse crear(@NotNull @Valid UsuarioCreateRequest request) {
        Sucursal sucursal = buscarSucursalActiva(request.sucursalId());
        String cedula = request.cedula().trim();
        if (usuarioRepository.existsByCedula(cedula)) {
            throw new ConflictoException("Ya existe un usuario con esa cedula.");
        }
        UsuarioCreateRequest normalizado = new UsuarioCreateRequest(request.sucursalId(), cedula,
            request.nombre().trim(), textoOpcional(request.apellido()), textoOpcional(request.telefono()),
            textoOpcional(request.email()), request.username().trim(), request.rol(), request.activo(), request.password());

        String keycloakId = keycloak.crearDeshabilitado(normalizado);
        compensarSiRollback(keycloakId);
        keycloak.asignarRolInicial(keycloakId, request.rol().name());
        Usuario usuario = guardar(usuarioMapper.toEntity(normalizado, sucursal, keycloakId));
        despuesCommit(usuario.getId(), () -> keycloak.cambiarEstado(keycloakId, request.activo()));
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid UsuarioUpdateRequest request) {
        Usuario usuario = buscarUsuario(id);
        Sucursal sucursal = buscarSucursalActiva(request.sucursalId());
        String cedula = request.cedula().trim();
        if (usuarioRepository.existsByCedulaAndIdNot(cedula, id)) {
            throw new ConflictoException("Ya existe otro usuario con esa cedula.");
        }
        UsuarioUpdateRequest normalizado = new UsuarioUpdateRequest(request.sucursalId(), cedula,
            request.nombre().trim(), textoOpcional(request.apellido()), textoOpcional(request.telefono()),
            textoOpcional(request.email()));
        usuarioMapper.updateEntity(normalizado, sucursal, usuario);
        UsuarioResponse response = usuarioMapper.toResponse(guardar(usuario));
        despuesCommit(id, () -> keycloak.actualizarPerfil(response));
        return response;
    }

    @Transactional
    public UsuarioResponse cambiarEstado(@NotNull @Positive Long id, boolean activo) {
        Usuario usuario = buscarUsuario(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!activo && authentication != null && authentication.getPrincipal() instanceof Jwt jwt
            && usuario.getKeycloakId().equals(jwt.getSubject())) {
            throw new ConflictoException("No puedes desactivar tu propia cuenta desde esta operacion.");
        }
        if (activo && !Boolean.TRUE.equals(usuario.getSucursal().getActiva())) {
            throw new ConflictoException("La sucursal del usuario esta desactivada.");
        }
        usuario.setActivo(activo);
        UsuarioResponse response = usuarioMapper.toResponse(guardar(usuario));
        despuesCommit(id, () -> keycloak.cambiarEstado(response.keycloakId(), activo));
        return response;
    }

    private Sucursal buscarSucursalActiva(Long id) {
        Sucursal sucursal = sucursalRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sucursal con ID " + id + "."));
        if (!Boolean.TRUE.equals(sucursal.getActiva())) {
            throw new ConflictoException("No se puede asignar el usuario a una sucursal desactivada.");
        }
        return sucursal;
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con ID " + id + "."));
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Usuario guardar(Usuario usuario) {
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                    && ("usuario_cedula_key".equals(violation.getConstraintName())
                        || "usuario_keycloak_id_key".equals(violation.getConstraintName()))) {
                    throw new ConflictoException("La cedula o la cuenta de Keycloak ya estan vinculadas a otro usuario.");
                }
            }
            throw exception;
        }
    }

    private void compensarSiRollback(String keycloakId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        keycloak.eliminarCuentaNoPersistida(keycloakId);
                    } catch (RuntimeException failure) {
                        log.error("Revisar cuenta Keycloak {}: no se pudo compensar el alta cancelada.", keycloakId);
                    }
                }
            }
        });
    }

    private void despuesCommit(Long id, Runnable operacion) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    operacion.run();
                } catch (RuntimeException failure) {
                    throw new ServicioExternoException("El usuario " + id
                        + " se guardo localmente, pero falta sincronizar Keycloak. Reintenta la actualizacion de perfil o estado.");
                }
            }
        });
    }
}
