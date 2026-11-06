package com.boutique.backend.inventario.service;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.repository.SucursalRepository;
import com.boutique.backend.inventario.dto.InventarioCreateRequest;
import com.boutique.backend.inventario.dto.InventarioLimitesRequest;
import com.boutique.backend.inventario.dto.InventarioResponse;
import com.boutique.backend.inventario.entity.Inventario;
import com.boutique.backend.inventario.mapper.InventarioMapper;
import com.boutique.backend.inventario.repository.InventarioRepository;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.producto.repository.VarianteProductoRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
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
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final SucursalRepository sucursalRepository;
    private final VarianteProductoRepository varianteRepository;
    private final InventarioMapper mapper;

    public Page<InventarioResponse> listar(@Positive Long sucursalId, @Positive Long varianteId,
        @NotNull Pageable pageable) {
        Page<Inventario> pagina;
        if (sucursalId != null && varianteId != null) {
            pagina = inventarioRepository.findBySucursalIdAndVarianteId(sucursalId, varianteId, pageable);
        } else if (sucursalId != null) {
            pagina = inventarioRepository.findBySucursalId(sucursalId, pageable);
        } else if (varianteId != null) {
            pagina = inventarioRepository.findByVarianteId(varianteId, pageable);
        } else {
            pagina = inventarioRepository.findAll(pageable);
        }
        return pagina.map(mapper::toResponse);
    }

    public InventarioResponse obtenerPorId(@NotNull @Positive Long id) {
        return mapper.toResponse(inventarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el inventario con ID " + id + ".")));
    }

    @Transactional
    public InventarioResponse crear(@NotNull @Valid InventarioCreateRequest request) {
        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sucursal indicada."));
        VarianteProducto variante = varianteRepository.findById(request.varianteId())
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la variante indicada."));
        if (!Boolean.TRUE.equals(sucursal.getActiva()) || !Boolean.TRUE.equals(variante.getActivo())
            || !Boolean.TRUE.equals(variante.getProducto().getActivo())) {
            throw new ConflictoException("La sucursal, el producto y su variante deben estar activos.");
        }
        if (inventarioRepository.existsByVarianteIdAndSucursalId(request.varianteId(), request.sucursalId())) {
            throw new ConflictoException("La variante ya tiene inventario en esta sucursal.");
        }
        try {
            return mapper.toResponse(inventarioRepository.saveAndFlush(mapper.toEntity(request, variante, sucursal)));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictoException("No se pudo crear el inventario; comprueba si la variante ya existe en la sucursal.", exception);
        }
    }

    @Transactional
    public InventarioResponse actualizarLimites(@NotNull @Positive Long id,
        @NotNull @Valid InventarioLimitesRequest request) {
        Inventario inventario = inventarioRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el inventario con ID " + id + "."));
        mapper.updateLimites(request, inventario);
        return mapper.toResponse(inventarioRepository.save(inventario));
    }
}
