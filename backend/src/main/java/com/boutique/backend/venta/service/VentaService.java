package com.boutique.backend.venta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.boutique.backend.caja.entity.SesionCaja;
import com.boutique.backend.caja.service.CajaVentasService;
import com.boutique.backend.cliente.entity.Cliente;
import com.boutique.backend.cliente.repository.ClienteRepository;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.inventario.service.StockService;
import com.boutique.backend.moneda.entity.Moneda;
import com.boutique.backend.moneda.repository.MonedaRepository;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.producto.repository.VarianteProductoRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.usuario.entity.Usuario;
import com.boutique.backend.usuario.repository.UsuarioRepository;
import com.boutique.backend.venta.dto.DetalleVentaRequest;
import com.boutique.backend.venta.dto.VentaRequest;
import com.boutique.backend.venta.dto.VentaResponse;
import com.boutique.backend.venta.dto.VentaResumenResponse;
import com.boutique.backend.venta.entity.DetalleVenta;
import com.boutique.backend.venta.entity.EstadoVenta;
import com.boutique.backend.venta.entity.Venta;
import com.boutique.backend.venta.mapper.DetalleVentaMapper;
import com.boutique.backend.venta.mapper.VentaMapper;
import com.boutique.backend.venta.repository.DetalleVentaRepository;
import com.boutique.backend.venta.repository.SerieFacturaRepository;
import com.boutique.backend.venta.repository.VentaRepository;
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
public class VentaService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final ClienteRepository clienteRepository;
    private final MonedaRepository monedaRepository;
    private final VarianteProductoRepository varianteRepository;
    private final SerieFacturaRepository serieRepository;
    private final NumeracionFacturaService numeracionService;
    private final CajaVentasService cajaService;
    private final StockService stockService;
    private final CalculadoraVenta calculadora;
    private final VentaMapper ventaMapper;
    private final DetalleVentaMapper detalleMapper;

    public Page<VentaResumenResponse> listar(@Positive Long sucursalId, @Positive Long sesionCajaId,
        EstadoVenta estado, @NotNull Pageable pageable) {
        return ventaRepository.buscar(sucursalId, sesionCajaId, estado, pageable).map(ventaMapper::toResumen);
    }

    public VentaResponse obtenerPorId(@NotNull @Positive Long id) {
        Venta venta = ventaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta con ID " + id + "."));
        return respuesta(venta, detalleRepository.findByVentaIdOrderByIdAsc(id));
    }

    @Transactional
    public VentaResponse crear(@NotNull @Valid VentaRequest request) {
        Usuario usuario = usuarioActual();
        Sucursal sucursal = sucursalRepository.findById(usuario.getSucursal().getId())
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sucursal del usuario."));
        if (!Boolean.TRUE.equals(sucursal.getActiva())) {
            throw new ConflictoException("La sucursal debe estar activa para vender.");
        }
        SesionCaja caja = cajaService.obtenerParaVenta(request.sesionCajaId(), usuario.getId(), sucursal.getId());
        // Lectura compartida: permite ventas concurrentes y evita editar esta moneda durante el cobro.
        Moneda moneda = monedaRepository.findByIdParaVenta(request.monedaId())
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la moneda indicada."));
        if (!Boolean.TRUE.equals(moneda.getActivo())) {
            throw new ConflictoException("La moneda debe estar activa.");
        }
        BigDecimal tipoCambio = request.tipoCambioAplicado().setScale(4, RoundingMode.UNNECESSARY);
        if ("NIO".equalsIgnoreCase(moneda.getCodigo()) && tipoCambio.compareTo(BigDecimal.ONE) != 0) {
            throw new ConflictoException("Las ventas en córdobas deben usar un tipo de cambio de 1.");
        }
        Cliente cliente = buscarCliente(request.clienteId());
        Map<Long, Integer> cantidades = acumularCantidades(request.detalles());
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
        var calculo = calculadora.calcular(request.detalles(), variantes, tipoCambio,
            sucursal.getEmpresa().getPorcentajeIva(), request.metodoPago(), request.importeRecibido());

        // El orden por variante es estable. Todos los cambios se revierten si cualquier paso falla.
        cantidades.forEach((varianteId, cantidad) -> stockService.retirar(varianteId, sucursal.getId(), cantidad));
        var numero = numeracionService.reservar(request.serieFacturaId(), sucursal.getId());
        cajaService.registrarVenta(caja, calculo.totalBase(), request.metodoPago());

        Venta venta = new Venta();
        venta.setSucursal(sucursal);
        venta.setUsuario(usuario);
        venta.setCliente(cliente);
        venta.setMoneda(moneda);
        venta.setSerieFactura(serieRepository.getReferenceById(request.serieFacturaId()));
        venta.setSesionCaja(caja);
        venta.setNoFactura(numero.numero());
        venta.setNumeroFactura(numero.numeroFactura());
        venta.setSubtotal(calculo.subtotal());
        venta.setDescuento(calculo.descuento());
        venta.setIva(calculo.iva());
        venta.setTotal(calculo.total());
        venta.setMetodoPago(request.metodoPago());
        venta.setTipoCambioAplicado(tipoCambio);
        venta.setImporteRecibido(calculo.importeRecibido());
        venta.setCambio(calculo.cambio());
        try {
            Venta guardada = ventaRepository.saveAndFlush(venta);
            List<DetalleVenta> detalles = calculo.lineas().stream().map(linea -> {
                DetalleVenta detalle = new DetalleVenta();
                detalle.setVenta(guardada);
                detalle.setVariante(linea.variante());
                detalle.setCantidad(linea.cantidad());
                detalle.setPrecioUnitario(linea.precioUnitario());
                detalle.setDescuento(linea.descuento());
                detalle.setMonto(linea.monto());
                return detalle;
            }).toList();
            List<DetalleVenta> guardados = detalleRepository.saveAllAndFlush(detalles);
            return respuesta(guardada, guardados);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictoException("No se pudo guardar la venta. Comprueba la serie y el número de factura.", exception);
        }
    }

    private Map<Long, Integer> acumularCantidades(List<DetalleVentaRequest> detalles) {
        Map<Long, Integer> cantidades = new TreeMap<>();
        try {
            for (DetalleVentaRequest detalle : detalles) {
                cantidades.merge(detalle.varianteId(), detalle.cantidad(), Math::addExact);
            }
        } catch (ArithmeticException exception) {
            throw new ConflictoException("La cantidad acumulada de una variante supera el límite permitido.", exception);
        }
        return cantidades;
    }

    private Cliente buscarCliente(Long id) {
        if (id == null) {
            return null;
        }
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente indicado."));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ConflictoException("El cliente debe estar activo.");
        }
        return cliente;
    }

    private Usuario usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Se requiere un usuario autenticado para vender.");
        }
        Usuario usuario = usuarioRepository.findByKeycloakId(jwt.getToken().getSubject())
            .orElseThrow(() -> new AccessDeniedException("El usuario debe estar registrado en el sistema."));
        if (!Boolean.TRUE.equals(usuario.getActivo()) || usuario.getSucursal() == null) {
            throw new AccessDeniedException("El usuario debe estar activo y tener una sucursal asignada.");
        }
        return usuario;
    }

    private VentaResponse respuesta(Venta venta, List<DetalleVenta> detalles) {
        return ventaMapper.toResponse(venta, detalles.stream().map(detalleMapper::toResponse).toList());
    }
}
