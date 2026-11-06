package com.boutique.backend.inventario.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.inventario.dto.DetalleIngresoRequest;
import com.boutique.backend.inventario.dto.IngresoRequest;
import com.boutique.backend.inventario.dto.IngresoResponse;
import com.boutique.backend.inventario.dto.IngresoResumenResponse;
import com.boutique.backend.inventario.entity.DetalleIngreso;
import com.boutique.backend.inventario.entity.Ingreso;
import com.boutique.backend.inventario.mapper.DetalleIngresoMapper;
import com.boutique.backend.inventario.mapper.IngresoMapper;
import com.boutique.backend.inventario.repository.DetalleIngresoRepository;
import com.boutique.backend.inventario.repository.IngresoRepository;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.producto.repository.VarianteProductoRepository;
import com.boutique.backend.proveedor.entity.Proveedor;
import com.boutique.backend.proveedor.repository.ProveedorRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
public class IngresoService {

    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("9999999999.99");

    private final IngresoRepository ingresoRepository;
    private final DetalleIngresoRepository detalleRepository;
    private final SucursalRepository sucursalRepository;
    private final VarianteProductoRepository varianteRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final IngresoMapper ingresoMapper;
    private final DetalleIngresoMapper detalleMapper;
    private final StockService stockService;

    public Page<IngresoResumenResponse> listar(@Positive Long sucursalId, @NotNull Pageable pageable) {
        Page<Ingreso> pagina = sucursalId == null ? ingresoRepository.findAll(pageable)
            : ingresoRepository.findBySucursalId(sucursalId, pageable);
        return pagina.map(ingresoMapper::toResumen);
    }

    public IngresoResponse obtenerPorId(@NotNull @Positive Long id) {
        Ingreso ingreso = ingresoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el ingreso con ID " + id + "."));
        return respuesta(ingreso, detalleRepository.findByIngresoIdOrderByIdAsc(id));
    }

    @Transactional
    public IngresoResponse crear(@NotNull @Valid IngresoRequest request) {
        Usuario usuario = usuarioActual();
        validarSucursalUsuario(usuario, request.sucursalId());
        Sucursal sucursal = bloquearSucursal(request.sucursalId());
        Proveedor proveedor = buscarProveedor(request.proveedorId());
        if (ingresoRepository.existsBySucursalIdAndProveedorIdAndNoDocumentoAndActivoTrue(
            sucursal.getId(), request.proveedorId(), request.noDocumento().trim())) {
            throw new ConflictoException("Ya existe un ingreso activo con ese documento y proveedor en la sucursal.");
        }

        Map<Long, Integer> cantidades = new TreeMap<>();
        List<BigDecimal> montos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (DetalleIngresoRequest detalle : request.detalles()) {
            acumular(cantidades, detalle.varianteId(), detalle.cantidad());
            BigDecimal monto = detalle.costoUnitario().multiply(BigDecimal.valueOf(detalle.cantidad()))
                .setScale(2, RoundingMode.UNNECESSARY);
            validarMonto(monto);
            montos.add(monto);
            total = total.add(monto);
            validarMonto(total);
        }
        Map<Long, VarianteProducto> variantes = varianteRepository.findAllById(cantidades.keySet()).stream()
            .collect(Collectors.toMap(VarianteProducto::getId, Function.identity()));
        for (Long id : cantidades.keySet()) {
            VarianteProducto variante = variantes.get(id);
            if (variante == null) {
                throw new RecursoNoEncontradoException("No existe la variante con ID " + id + ".");
            }
            if (!Boolean.TRUE.equals(variante.getActivo()) || !Boolean.TRUE.equals(variante.getProducto().getActivo())) {
                throw new ConflictoException("El producto y la variante " + id + " deben estar activos.");
            }
        }

        Ingreso ingreso = ingresoRepository.save(ingresoMapper.toEntity(request, sucursal, usuario, proveedor, total));
        List<DetalleIngreso> detalles = new ArrayList<>();
        for (int index = 0; index < request.detalles().size(); index++) {
            DetalleIngresoRequest detalle = request.detalles().get(index);
            detalles.add(detalleMapper.toEntity(detalle, ingreso, variantes.get(detalle.varianteId()), montos.get(index)));
        }
        List<DetalleIngreso> guardados = detalleRepository.saveAll(detalles);
        cantidades.forEach((varianteId, cantidad) -> stockService.ingresar(varianteId, sucursal.getId(), cantidad));
        return respuesta(ingreso, guardados);
    }

    @Transactional
    public void anular(@NotNull @Positive Long id) {
        Usuario usuario = usuarioActual();
        Sucursal sucursal = bloquearSucursal(usuario.getSucursal().getId());
        Ingreso ingreso = ingresoRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el ingreso con ID " + id + "."));
        validarSucursalUsuario(usuario, ingreso.getSucursal().getId());
        if (!Boolean.TRUE.equals(ingreso.getActivo())) {
            return;
        }
        Map<Long, Integer> cantidades = new TreeMap<>();
        for (DetalleIngreso detalle : detalleRepository.findByIngresoIdOrderByIdAsc(id)) {
            acumular(cantidades, detalle.getVariante().getId(), detalle.getCantidad());
        }
        cantidades.forEach((varianteId, cantidad) -> stockService.retirar(varianteId, sucursal.getId(), cantidad));
        ingreso.setActivo(false);
        ingresoRepository.save(ingreso);
    }

    private Usuario usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Se requiere un usuario autenticado para registrar movimientos.");
        }
        Usuario usuario = usuarioRepository.findByKeycloakId(jwt.getToken().getSubject())
            .orElseThrow(() -> new AccessDeniedException("El usuario debe estar registrado en el sistema."));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getSucursal() == null) {
            throw new AccessDeniedException("El usuario debe estar activo y tener una sucursal asignada.");
        }
        return usuario;
    }

    private void validarSucursalUsuario(Usuario usuario, Long sucursalId) {
        if (!usuario.getSucursal().getId().equals(sucursalId)) {
            throw new AccessDeniedException("Solo puedes registrar o anular ingresos de tu sucursal.");
        }
    }

    private Sucursal bloquearSucursal(Long id) {
        Sucursal sucursal = sucursalRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sucursal indicada."));
        if (!Boolean.TRUE.equals(sucursal.getActiva())) {
            throw new ConflictoException("La sucursal debe estar activa.");
        }
        return sucursal;
    }

    private Proveedor buscarProveedor(Long id) {
        if (id == null) {
            return null;
        }
        Proveedor proveedor = proveedorRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el proveedor indicado."));
        if (!Boolean.TRUE.equals(proveedor.getActivo())) {
            throw new ConflictoException("El proveedor debe estar activo.");
        }
        return proveedor;
    }

    private void acumular(Map<Long, Integer> cantidades, Long varianteId, int cantidad) {
        if (cantidad <= 0) {
            throw new ConflictoException("La cantidad debe ser mayor que cero.");
        }
        try {
            cantidades.merge(varianteId, cantidad, Math::addExact);
        } catch (ArithmeticException exception) {
            throw new ConflictoException("La cantidad acumulada supera el límite permitido.", exception);
        }
    }

    private void validarMonto(BigDecimal monto) {
        if (monto.signum() < 0 || monto.compareTo(MONTO_MAXIMO) > 0) {
            throw new ConflictoException("El importe supera el rango permitido por la base de datos.");
        }
    }

    private IngresoResponse respuesta(Ingreso ingreso, List<DetalleIngreso> detalles) {
        return ingresoMapper.toResponse(ingreso, detalles.stream().map(detalleMapper::toResponse).toList());
    }
}
