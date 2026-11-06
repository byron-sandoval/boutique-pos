package com.boutique.backend.venta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.boutique.backend.caja.entity.SesionCaja;
import com.boutique.backend.caja.service.CajaVentasService;
import com.boutique.backend.inventario.service.StockService;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import com.boutique.backend.venta.dto.VentaAnulacionRequest;
import com.boutique.backend.venta.dto.VentaResponse;
import com.boutique.backend.venta.entity.DetalleVenta;
import com.boutique.backend.venta.entity.EstadoVenta;
import com.boutique.backend.venta.entity.Venta;
import com.boutique.backend.venta.mapper.DetalleVentaMapper;
import com.boutique.backend.venta.mapper.VentaMapper;
import com.boutique.backend.venta.repository.DetalleVentaRepository;
import com.boutique.backend.venta.repository.DevolucionRepository;
import com.boutique.backend.venta.repository.VentaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
public class VentaAnulacionService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleRepository;
    private final DevolucionRepository devolucionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaVentasService cajaService;
    private final StockService stockService;
    private final VentaMapper ventaMapper;
    private final DetalleVentaMapper detalleMapper;

    @Transactional
    public VentaResponse anular(@NotNull @Positive Long id, @NotNull @Valid VentaAnulacionRequest request) {
        Usuario usuario = usuarioActual();
        // Primero se bloquea la caja, como al registrar ventas. La proyección no carga una venta sin bloquear.
        Long cajaId = ventaRepository.findSesionCajaId(id).orElseThrow(() -> noEncontrada(id));
        SesionCaja caja = cajaService.obtenerParaAnulacion(cajaId, usuario.getSucursal().getId());
        Venta venta = ventaRepository.findByIdForUpdate(id).orElseThrow(() -> noEncontrada(id));
        if (!venta.getSucursal().getId().equals(usuario.getSucursal().getId())
            || !venta.getSesionCaja().getId().equals(caja.getId())) {
            throw new AccessDeniedException("La venta no pertenece a la caja indicada o a tu sucursal.");
        }
        List<DetalleVenta> detalles = detalleRepository.findByVentaIdOrderByIdAsc(id);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            return respuesta(venta, detalles);
        }
        if (devolucionRepository.existsByVentaId(id)) {
            throw new ConflictoException("No puedes anular una venta que ya tiene devoluciones registradas.");
        }
        if (detalles.isEmpty()) {
            throw new ConflictoException("No se puede anular una venta sin detalles.");
        }
        Map<Long, Integer> cantidades = cantidadesPorVariante(detalles);
        // Se revierte exactamente el importe contabilizado al vender, usando el tipo de cambio histórico.
        BigDecimal importeBase = venta.getTotal().multiply(venta.getTipoCambioAplicado())
            .setScale(2, RoundingMode.HALF_UP);
        cajaService.anularVenta(caja, importeBase, venta.getMetodoPago());
        cantidades.forEach((varianteId, cantidad) -> stockService.ingresar(varianteId, venta.getSucursal().getId(), cantidad));
        venta.setEstado(EstadoVenta.ANULADA);
        venta.setMotivoAnulacion(request.motivo().trim());
        venta.setAnuladaEn(Instant.now());
        venta.setAnuladaPor(usuario);
        return respuesta(ventaRepository.saveAndFlush(venta), detalles);
    }

    private Map<Long, Integer> cantidadesPorVariante(List<DetalleVenta> detalles) {
        Map<Long, Integer> cantidades = new TreeMap<>();
        try {
            for (DetalleVenta detalle : detalles) {
                if (detalle.getCantidad() <= 0) {
                    throw new ConflictoException("La venta contiene cantidades inválidas.");
                }
                cantidades.merge(detalle.getVariante().getId(), detalle.getCantidad(), Math::addExact);
            }
        } catch (ArithmeticException exception) {
            throw new ConflictoException("La cantidad a reintegrar supera el límite permitido.", exception);
        }
        return cantidades;
    }

    private Usuario usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Se requiere un usuario autenticado para anular ventas.");
        }
        if (jwt.getAuthorities().stream().noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Solo el ADMIN puede anular ventas.");
        }
        Usuario usuario = usuarioRepository.findByKeycloakId(jwt.getToken().getSubject())
            .orElseThrow(() -> new AccessDeniedException("El usuario debe estar registrado en el sistema."));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getSucursal() == null) {
            throw new AccessDeniedException("El usuario debe estar activo y tener una sucursal asignada.");
        }
        return usuario;
    }

    private RecursoNoEncontradoException noEncontrada(Long id) {
        return new RecursoNoEncontradoException("No existe la venta con ID " + id + ".");
    }

    private VentaResponse respuesta(Venta venta, List<DetalleVenta> detalles) {
        return ventaMapper.toResponse(venta, detalles.stream().map(detalleMapper::toResponse).toList());
    }
}
