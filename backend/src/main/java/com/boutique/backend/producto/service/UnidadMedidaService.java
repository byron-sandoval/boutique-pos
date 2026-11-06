package com.boutique.backend.producto.service;

import com.boutique.backend.producto.dto.UnidadMedidaRequest;
import com.boutique.backend.producto.dto.UnidadMedidaResponse;
import com.boutique.backend.producto.entity.UnidadMedida;
import com.boutique.backend.producto.mapper.UnidadMedidaMapper;
import com.boutique.backend.producto.repository.UnidadMedidaRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;
    private final UnidadMedidaMapper unidadMedidaMapper;

    public Page<UnidadMedidaResponse> listar(@NotNull Pageable pageable) {
        return unidadMedidaRepository.findAll(pageable).map(unidadMedidaMapper::toResponse);
    }

    public UnidadMedidaResponse obtenerPorId(@NotNull @Positive Long id) {
        return unidadMedidaMapper.toResponse(buscar(id));
    }

    @Transactional
    public UnidadMedidaResponse crear(@NotNull @Valid UnidadMedidaRequest request) {
        String nombre = request.nombre().trim();
        validarNombre(nombre, null);
        UnidadMedida entity = unidadMedidaMapper.toEntity(request);
        entity.setNombre(nombre);
        return unidadMedidaMapper.toResponse(guardar(entity));
    }

    @Transactional
    public UnidadMedidaResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid UnidadMedidaRequest request) {
        UnidadMedida entity = buscar(id);
        String nombre = request.nombre().trim();
        validarNombre(nombre, id);
        unidadMedidaMapper.updateEntity(request, entity);
        entity.setNombre(nombre);
        return unidadMedidaMapper.toResponse(guardar(entity));
    }

    @Transactional
    public UnidadMedidaResponse cambiarEstado(@NotNull @Positive Long id, boolean activo) {
        UnidadMedida entity = buscar(id);
        entity.setActivo(activo);
        return unidadMedidaMapper.toResponse(guardar(entity));
    }

    private void validarNombre(String nombre, Long id) {
        boolean duplicado = id == null
            ? unidadMedidaRepository.existsByNombre(nombre)
            : unidadMedidaRepository.existsByNombreAndIdNot(nombre, id);
        if (duplicado) {
            throw new ConflictoException("Ya existe otra unidad de medida con ese nombre.");
        }
    }

    private UnidadMedida buscar(Long id) {
        return unidadMedidaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la unidad de medida con ID " + id + "."));
    }

    private UnidadMedida guardar(UnidadMedida entity) {
        return CatalogoSupport.guardar(() -> unidadMedidaRepository.saveAndFlush(entity));
    }
}
