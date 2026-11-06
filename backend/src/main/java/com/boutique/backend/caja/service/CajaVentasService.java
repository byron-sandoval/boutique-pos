package com.boutique.backend.caja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.boutique.backend.caja.entity.EstadoCaja;
import com.boutique.backend.caja.entity.SesionCaja;
import com.boutique.backend.caja.repository.SesionCajaRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.venta.entity.MetodoPago;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CajaVentasService {

    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("999999999999.99");
    private final SesionCajaRepository cajaRepository;

    public SesionCaja obtenerParaVenta(Long id, Long usuarioId, Long sucursalId) {
        SesionCaja caja = cajaRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sesión de caja indicada."));
        if (!caja.getAbiertoPor().getId().equals(usuarioId) || !caja.getSucursal().getId().equals(sucursalId)) {
            throw new AccessDeniedException("Solo puedes operar tu propia caja en tu sucursal.");
        }
        validarAbierta(caja);
        return caja;
    }

    // Uso interno de la anulación autorizada para ADMIN; conserva el bloqueo de caja y la misma sucursal.
    public SesionCaja obtenerParaAnulacion(Long id, Long sucursalId) {
        SesionCaja caja = cajaRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sesión de caja indicada."));
        if (!caja.getSucursal().getId().equals(sucursalId)) {
            throw new AccessDeniedException("Solo puedes anular ventas de cajas de tu sucursal.");
        }
        validarAbierta(caja);
        return caja;
    }

    // La caja debe proceder de obtenerParaVenta dentro de la misma transacción de venta.
    public void registrarVenta(SesionCaja caja, BigDecimal importeBase, MetodoPago metodoPago) {
        validarAbierta(caja);
        BigDecimal importe = importeBase.setScale(2, RoundingMode.UNNECESSARY);
        if (importe.signum() < 0) {
            throw new ConflictoException("El importe de la venta no puede ser negativo.");
        }
        BigDecimal totalVentas = caja.getTotalVentas().add(importe);
        BigDecimal efectivo = caja.getTotalEfectivo();
        BigDecimal tarjeta = caja.getTotalTarjeta();
        BigDecimal transferencia = caja.getTotalTransferencia();
        switch (metodoPago) {
            case EFECTIVO -> efectivo = efectivo.add(importe);
            case TARJETA -> tarjeta = tarjeta.add(importe);
            case TRANSFERENCIA -> transferencia = transferencia.add(importe);
        }
        validarRango(totalVentas);
        validarRango(efectivo);
        validarRango(tarjeta);
        validarRango(transferencia);
        validarRango(caja.getEfectivoInicial().add(efectivo));
        int cantidadVentas;
        try {
            cantidadVentas = Math.addExact(caja.getCantidadVentas(), 1);
        } catch (ArithmeticException exception) {
            throw new ConflictoException("La cantidad de ventas de esta caja alcanzó su límite.", exception);
        }
        caja.setTotalVentas(totalVentas);
        caja.setTotalEfectivo(efectivo);
        caja.setTotalTarjeta(tarjeta);
        caja.setTotalTransferencia(transferencia);
        caja.setCantidadVentas(cantidadVentas);
        cajaRepository.save(caja);
    }

    // La caja debe estar bloqueada por obtenerParaAnulacion en esta misma transacción.
    public void anularVenta(SesionCaja caja, BigDecimal importeBase, MetodoPago metodoPago) {
        validarAbierta(caja);
        BigDecimal importe = importeBase.setScale(2, RoundingMode.UNNECESSARY);
        if (importe.signum() < 0) {
            throw new ConflictoException("El importe de la anulación no puede ser negativo.");
        }
        BigDecimal totalVentas = caja.getTotalVentas().subtract(importe);
        BigDecimal efectivo = caja.getTotalEfectivo();
        BigDecimal tarjeta = caja.getTotalTarjeta();
        BigDecimal transferencia = caja.getTotalTransferencia();
        switch (metodoPago) {
            case EFECTIVO -> efectivo = efectivo.subtract(importe);
            case TARJETA -> tarjeta = tarjeta.subtract(importe);
            case TRANSFERENCIA -> transferencia = transferencia.subtract(importe);
        }
        if (totalVentas.signum() < 0 || efectivo.signum() < 0 || tarjeta.signum() < 0
            || transferencia.signum() < 0 || caja.getCantidadVentas() <= 0) {
            throw new ConflictoException("Los acumulados de caja no permiten revertir esta venta.");
        }
        caja.setTotalVentas(totalVentas);
        caja.setTotalEfectivo(efectivo);
        caja.setTotalTarjeta(tarjeta);
        caja.setTotalTransferencia(transferencia);
        caja.setCantidadVentas(caja.getCantidadVentas() - 1);
        cajaRepository.save(caja);
    }

    private void validarAbierta(SesionCaja caja) {
        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new ConflictoException("La caja debe estar abierta para registrar o anular ventas.");
        }
    }

    private void validarRango(BigDecimal importe) {
        if (importe.compareTo(MONTO_MAXIMO) > 0) {
            throw new ConflictoException("Los acumulados de caja superan el importe permitido por la base de datos.");
        }
    }
}
