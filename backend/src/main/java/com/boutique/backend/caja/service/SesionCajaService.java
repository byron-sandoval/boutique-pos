package com.boutique.backend.caja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.boutique.backend.caja.dto.SesionCajaAperturaRequest;
import com.boutique.backend.caja.dto.SesionCajaCierreRequest;
import com.boutique.backend.caja.dto.SesionCajaResponse;
import com.boutique.backend.caja.entity.EstadoCaja;
import com.boutique.backend.caja.entity.SesionCaja;
import com.boutique.backend.caja.mapper.SesionCajaMapper;
import com.boutique.backend.caja.repository.SesionCajaRepository;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SesionCajaService {

    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("999999999999.99");

    private final SesionCajaRepository cajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final SesionCajaMapper mapper;

    public Page<SesionCajaResponse> listar(@Positive Long sucursalId, @Positive Long usuarioId, EstadoCaja estado,
        @NotNull Pageable pageable) {
        Usuario usuario = usuarioActual();
        boolean admin = administradorConsulta(usuario);
        Long sucursalPermitida = usuario.getSucursal().getId();
        if (sucursalId != null && !sucursalPermitida.equals(sucursalId)) {
            throw new AccessDeniedException("Solo puedes consultar cajas de tu sucursal.");
        }
        if (!admin && usuarioId != null && !usuario.getId().equals(usuarioId)) {
            throw new AccessDeniedException("Solo puedes consultar tus propias cajas.");
        }
        Long usuarioPermitido = admin ? usuarioId : usuario.getId();
        return cajaRepository.buscar(sucursalPermitida, usuarioPermitido, estado, pageable).map(mapper::toResponse);
    }

    public SesionCajaResponse obtenerPorId(@NotNull @Positive Long id) {
        Usuario usuario = usuarioActual();
        Long usuarioPermitido = administradorConsulta(usuario) ? null : usuario.getId();
        return mapper.toResponse(cajaRepository.findVisibleById(id, usuario.getSucursal().getId(), usuarioPermitido)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sesión de caja con ID " + id + ".")));
    }

    public SesionCajaResponse obtenerActual() {
        Usuario usuario = usuarioActual();
        administradorConsulta(usuario);
        return mapper.toResponse(cajaRepository.findByAbiertoPorIdAndSucursalIdAndEstado(
            usuario.getId(), usuario.getSucursal().getId(), EstadoCaja.ABIERTA)
            .orElseThrow(() -> new RecursoNoEncontradoException("No tienes una caja abierta.")));
    }

    @Transactional
    public SesionCajaResponse abrir(@NotNull @Valid SesionCajaAperturaRequest request) {
        Usuario usuario = usuarioActual();
        // Serializa las aperturas en la sucursal; el índice único protege además frente a otros escritores.
        Sucursal sucursal = sucursalRepository.findByIdForUpdate(usuario.getSucursal().getId())
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sucursal del usuario."));
        if (!Boolean.TRUE.equals(sucursal.getActiva())) {
            throw new ConflictoException("La sucursal debe estar activa para abrir una caja.");
        }
        if (cajaRepository.existsByAbiertoPorIdAndEstado(usuario.getId(), EstadoCaja.ABIERTA)) {
            throw new ConflictoException("Ya tienes una caja abierta. Ciérrala antes de abrir otra.");
        }
        try {
            return mapper.toResponse(cajaRepository.saveAndFlush(mapper.toEntity(request, sucursal, usuario)));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictoException("No se pudo abrir la caja; comprueba si ya tienes una sesión abierta.", exception);
        }
    }

    @Transactional
    public SesionCajaResponse cerrar(@NotNull @Positive Long id,
        @NotNull @Valid SesionCajaCierreRequest request) {
        Usuario usuario = usuarioActual();
        SesionCaja caja = cajaRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sesión de caja con ID " + id + "."));
        if (!caja.getAbiertoPor().getId().equals(usuario.getId())
            || !caja.getSucursal().getId().equals(usuario.getSucursal().getId())) {
            throw new AccessDeniedException("Solo puedes cerrar tu propia caja en tu sucursal.");
        }
        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new ConflictoException("La caja ya está cerrada y su arqueo no se puede modificar.");
        }
        // Ventas actualizará los acumulados bajo el mismo bloqueo de la sesión.
        BigDecimal esperado = caja.getEfectivoInicial().add(caja.getTotalEfectivo());
        if (esperado.compareTo(MONTO_MAXIMO) > 0) {
            throw new ConflictoException("El efectivo esperado supera el importe permitido por la base de datos.");
        }
        BigDecimal contado = request.efectivoCierre().setScale(2, RoundingMode.UNNECESSARY);
        caja.setEfectivoEsperado(esperado.setScale(2, RoundingMode.UNNECESSARY));
        caja.setEfectivoCierre(contado);
        caja.setDiferencia(contado.subtract(esperado));
        caja.setCerradoPor(usuario);
        caja.setCerradoEn(Instant.now());
        caja.setEstado(EstadoCaja.CERRADA);
        if (request.notas() != null && !request.notas().isBlank()) {
            String notaCierre = "Cierre: " + request.notas().trim();
            caja.setNotas(caja.getNotas() == null ? notaCierre : caja.getNotas() + "\n" + notaCierre);
        }
        return mapper.toResponse(cajaRepository.save(caja));
    }

    private boolean administradorConsulta(Usuario usuario) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean cajero = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CAJERO"));
        if ((!admin && !cajero) || !Boolean.TRUE.equals(usuario.getSucursal().getActiva())) {
            throw new AccessDeniedException("Se requiere ADMIN o CAJERO con una sucursal activa para consultar cajas.");
        }
        return admin;
    }

    private Usuario usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Se requiere un usuario autenticado para operar la caja.");
        }
        Usuario usuario = usuarioRepository.findByKeycloakId(jwt.getToken().getSubject())
            .orElseThrow(() -> new AccessDeniedException("El usuario debe estar registrado en el sistema."));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getSucursal() == null) {
            throw new AccessDeniedException("El usuario debe estar activo y tener una sucursal asignada.");
        }
        return usuario;
    }
}
