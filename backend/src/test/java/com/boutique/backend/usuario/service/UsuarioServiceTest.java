package com.boutique.backend.usuario.service;

import java.util.Optional;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.ServicioExternoException;
import com.boutique.backend.usuario.dto.RolUsuario;
import com.boutique.backend.usuario.dto.UsuarioCreateRequest;
import com.boutique.backend.usuario.dto.UsuarioUpdateRequest;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.keycloak.KeycloakAdminClient;
import com.boutique.backend.usuario.mapper.UsuarioMapper;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Simula los callbacks de commit/rollback: no abre transacciones contra una base.
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private SucursalRepository sucursales;
    @Mock private KeycloakAdminClient keycloak;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarios, sucursales, new UsuarioMapper(), keycloak);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
        SecurityContextHolder.clearContext();
    }

    @Test
    void noCreaCuentasParaSucursalesDesactivadas() {
        Sucursal sucursal = sucursal();
        sucursal.setActiva(false);
        when(sucursales.findById(1L)).thenReturn(Optional.of(sucursal));

        assertThrows(ConflictoException.class, () -> service.crear(request()));

        verifyNoInteractions(keycloak, usuarios);
    }

    @Test
    void rechazaCedulaDuplicadaAntesDeContactarKeycloak() {
        when(sucursales.findById(1L)).thenReturn(Optional.of(sucursal()));
        when(usuarios.existsByCedula("CED-1")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> service.crear(request()));

        verifyNoInteractions(keycloak);
    }

    @Test
    void vinculaLaCuentaYLaActivaSoloDespuesDelCommit() {
        prepararCuenta();
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(5L);
            return usuario;
        });

        var response = service.crear(request());

        assertEquals("kc-created", response.keycloakId());
        assertEquals(1L, response.sucursalId());
        verify(keycloak).asignarRolInicial("kc-created", "CAJERO");
        verify(keycloak, never()).cambiarEstado(anyString(), anyBoolean());
        commit();
        verify(keycloak).cambiarEstado("kc-created", true);
        verify(keycloak, never()).eliminarCuentaNoPersistida(anyString());
    }

    @Test
    void compensaLaCuentaSiFallaLaAsignacionDeRol() {
        prepararCuenta();
        doThrow(new ServicioExternoException("Rol no disponible"))
            .when(keycloak).asignarRolInicial("kc-created", "CAJERO");

        assertThrows(ServicioExternoException.class, () -> service.crear(request()));
        rollback();

        verify(keycloak).eliminarCuentaNoPersistida("kc-created");
        verify(usuarios, never()).saveAndFlush(any(Usuario.class));
        verify(keycloak, never()).cambiarEstado(anyString(), anyBoolean());
    }

    @Test
    void compensaLaCuentaSiFallaLaPersistenciaLocal() {
        prepararCuenta();
        when(usuarios.saveAndFlush(any(Usuario.class)))
            .thenThrow(new DataIntegrityViolationException("Fallo local"));

        assertThrows(DataIntegrityViolationException.class, () -> service.crear(request()));
        rollback();

        verify(keycloak).eliminarCuentaNoPersistida("kc-created");
        verify(keycloak, never()).cambiarEstado(anyString(), anyBoolean());
    }

    @Test
    void unFalloDespuesDelCommitNoEliminaUnaCuentaYaVinculada() {
        prepararCuenta();
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(5L);
            return usuario;
        });
        service.crear(request());
        doThrow(new ServicioExternoException("Sin conexion")).when(keycloak).cambiarEstado("kc-created", true);

        ServicioExternoException error = assertThrows(ServicioExternoException.class, this::commit);

        assertFalse(error.getMessage().isBlank());
        verify(keycloak, never()).eliminarCuentaNoPersistida(anyString());
    }

    @Test
    void actualizarConservaUsernameKeycloakIdYRolYEsperaElCommit() {
        Usuario usuario = usuario();
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(sucursales.findById(1L)).thenReturn(Optional.of(sucursal()));
        when(usuarios.saveAndFlush(usuario)).thenReturn(usuario);

        var response = service.actualizar(5L,
            new UsuarioUpdateRequest(1L, "CED-1", "Nuevo nombre", null, null, null));

        assertEquals("operador", response.username());
        assertEquals("kc-1", response.keycloakId());
        verifyNoInteractions(keycloak);
        commit();
        verify(keycloak).actualizarPerfil(response);
        verify(keycloak, never()).asignarRolInicial(anyString(), anyString());
    }

    @Test
    void desactivarNoBorraElUsuarioYSeSincronizaDespuesDelCommit() {
        Usuario usuario = usuario();
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(usuarios.saveAndFlush(usuario)).thenReturn(usuario);

        var response = service.cambiarEstado(5L, false);

        assertFalse(response.activo());
        assertEquals("kc-1", response.keycloakId());
        verifyNoInteractions(keycloak);
        commit();
        verify(keycloak).cambiarEstado("kc-1", false);
        verify(usuarios, never()).delete(any(Usuario.class));
        verify(usuarios, never()).deleteById(5L);
    }

    @Test
    void impideDesactivarLaPropiaCuenta() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario()));
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject("kc-1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThrows(ConflictoException.class, () -> service.cambiarEstado(5L, false));

        verifyNoInteractions(keycloak);
        verify(usuarios, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void laSolicitudNoExponeLaContrasenaEnToString() {
        assertFalse(request().toString().contains(request().password()));
    }

    private void prepararCuenta() {
        when(sucursales.findById(1L)).thenReturn(Optional.of(sucursal()));
        when(keycloak.crearDeshabilitado(any(UsuarioCreateRequest.class))).thenReturn("kc-created");
    }

    private void commit() {
        var callbacks = TransactionSynchronizationManager.getSynchronizations();
        try {
            callbacks.forEach(TransactionSynchronization::afterCommit);
        } finally {
            callbacks.forEach(callback -> callback.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        }
    }

    private void rollback() {
        TransactionSynchronizationManager.getSynchronizations().forEach(
            callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    }

    private Sucursal sucursal() {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(1L);
        return sucursal;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setKeycloakId("kc-1");
        usuario.setUsername("operador");
        usuario.setNombre("Operador");
        usuario.setCedula("CED-1");
        usuario.setSucursal(sucursal());
        return usuario;
    }

    private UsuarioCreateRequest request() {
        return new UsuarioCreateRequest(1L, "CED-1", "Operador", null, null, null,
            "operador", RolUsuario.CAJERO, true, "Temporal-prueba-123");
    }
}
