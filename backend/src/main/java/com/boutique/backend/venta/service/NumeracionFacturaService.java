package com.boutique.backend.venta.service;

import java.util.Locale;

import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.venta.entity.SerieFactura;
import com.boutique.backend.venta.repository.SerieFacturaRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class NumeracionFacturaService {

    private final SerieFacturaRepository serieRepository;

    // Solo se invoca desde la transacción de venta: un fallo de la venta también revierte el contador.
    public NumeroFactura reservar(@NotNull @Positive Long serieId, @NotNull @Positive Long sucursalId) {
        SerieFactura serie = serieRepository.findByIdForUpdate(serieId)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la serie de factura indicada."));
        if (!serie.getSucursal().getId().equals(sucursalId)) {
            throw new ConflictoException("La serie de factura pertenece a otra sucursal.");
        }
        if (!Boolean.TRUE.equals(serie.getActiva()) || !Boolean.TRUE.equals(serie.getSucursal().getActiva())) {
            throw new ConflictoException("La serie y su sucursal deben estar activas.");
        }
        long numero = serie.getSiguienteNumero();
        if (numero == Long.MAX_VALUE) {
            throw new ConflictoException("Se agotó la numeración de esta serie. Debes crear otra.");
        }
        String numeroFactura = serie.getPrefijo() + String.format(Locale.ROOT, "%0" + serie.getDigitos() + "d", numero);
        serie.setSiguienteNumero(numero + 1);
        serieRepository.save(serie);
        return new NumeroFactura(numero, numeroFactura);
    }

    public record NumeroFactura(long numero, String numeroFactura) {
    }
}
