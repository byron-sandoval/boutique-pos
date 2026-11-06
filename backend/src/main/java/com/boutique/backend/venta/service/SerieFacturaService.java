package com.boutique.backend.venta.service;

import java.util.Locale;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import com.boutique.backend.venta.dto.SerieFacturaCreateRequest;
import com.boutique.backend.venta.dto.SerieFacturaResponse;
import com.boutique.backend.venta.dto.SerieFacturaUpdateRequest;
import com.boutique.backend.venta.entity.SerieFactura;
import com.boutique.backend.venta.mapper.SerieFacturaMapper;
import com.boutique.backend.venta.repository.SerieFacturaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SerieFacturaService {

    private final SerieFacturaRepository serieRepository;
    private final SucursalRepository sucursalRepository;
    private final SerieFacturaMapper mapper;

    public Page<SerieFacturaResponse> listar(@Positive Long sucursalId, Boolean activa, @NotNull Pageable pageable) {
        return serieRepository.buscar(sucursalId, activa, pageable).map(mapper::toResponse);
    }

    public SerieFacturaResponse obtenerPorId(@NotNull @Positive Long id) {
        return mapper.toResponse(serieRepository.findById(id).orElseThrow(() -> noEncontrada(id)));
    }

    @Transactional
    public SerieFacturaResponse crear(@NotNull @Valid SerieFacturaCreateRequest request) {
        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sucursal indicada."));
        if (!Boolean.TRUE.equals(sucursal.getActiva())) {
            throw new ConflictoException("La sucursal debe estar activa para crear una serie.");
        }
        validarPrefijo(sucursal.getId(), request.prefijo().toUpperCase(Locale.ROOT), null);
        return mapper.toResponse(guardar(mapper.toEntity(request, sucursal)));
    }

    @Transactional
    public SerieFacturaResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid SerieFacturaUpdateRequest request) {
        SerieFactura serie = buscarParaActualizar(id);
        String prefijo = request.prefijo().toUpperCase(Locale.ROOT);
        validarPrefijo(serie.getSucursal().getId(), prefijo, id);
        boolean cambiaFormato = !serie.getPrefijo().equals(prefijo) || !serie.getDigitos().equals(request.digitos());
        if (cambiaFormato && serieRepository.contarVentas(id) > 0) {
            throw new ConflictoException("No puedes cambiar el formato de una serie que ya tiene ventas registradas.");
        }
        mapper.updateEntity(request, serie);
        return mapper.toResponse(guardar(serie));
    }

    @Transactional
    public SerieFacturaResponse cambiarEstado(@NotNull @Positive Long id, boolean activa) {
        SerieFactura serie = buscarParaActualizar(id);
        serie.setActiva(activa);
        return mapper.toResponse(guardar(serie));
    }

    private void validarPrefijo(Long sucursalId, String prefijo, Long id) {
        boolean duplicado = id == null ? serieRepository.existsBySucursalIdAndPrefijo(sucursalId, prefijo)
            : serieRepository.existsBySucursalIdAndPrefijoAndIdNot(sucursalId, prefijo, id);
        if (duplicado) {
            throw new ConflictoException("Ya existe otra serie con ese prefijo en la sucursal.");
        }
    }

    private SerieFactura buscarParaActualizar(Long id) {
        return serieRepository.findByIdForUpdate(id).orElseThrow(() -> noEncontrada(id));
    }

    private RecursoNoEncontradoException noEncontrada(Long id) {
        return new RecursoNoEncontradoException("No existe la serie de factura con ID " + id + ".");
    }

    private SerieFactura guardar(SerieFactura serie) {
        try {
            return serieRepository.saveAndFlush(serie);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictoException("No se pudo guardar la serie; comprueba que el prefijo no esté duplicado en la sucursal.", exception);
        }
    }
}
