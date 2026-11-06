package com.boutique.backend.producto.service;

import com.boutique.backend.producto.dto.CategoriaRequest;
import com.boutique.backend.producto.dto.CategoriaResponse;
import com.boutique.backend.producto.entity.Categoria;
import com.boutique.backend.producto.mapper.CategoriaMapper;
import com.boutique.backend.producto.repository.CategoriaRepository;
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
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    public Page<CategoriaResponse> listar(@NotNull Pageable pageable) {
        return categoriaRepository.findAll(pageable).map(categoriaMapper::toResponse);
    }

    public CategoriaResponse obtenerPorId(@NotNull @Positive Long id) {
        return categoriaMapper.toResponse(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(@NotNull @Valid CategoriaRequest request) {
        String nombre = request.nombre().trim();
        validarNombre(nombre, null);
        Categoria entity = categoriaMapper.toEntity(request);
        entity.setNombre(nombre);
        return categoriaMapper.toResponse(guardar(entity));
    }

    @Transactional
    public CategoriaResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid CategoriaRequest request) {
        Categoria entity = buscar(id);
        String nombre = request.nombre().trim();
        validarNombre(nombre, id);
        categoriaMapper.updateEntity(request, entity);
        entity.setNombre(nombre);
        return categoriaMapper.toResponse(guardar(entity));
    }

    @Transactional
    public CategoriaResponse cambiarEstado(@NotNull @Positive Long id, boolean activo) {
        Categoria entity = buscar(id);
        entity.setActivo(activo);
        return categoriaMapper.toResponse(guardar(entity));
    }

    private void validarNombre(String nombre, Long id) {
        boolean duplicado = id == null
            ? categoriaRepository.existsByNombre(nombre)
            : categoriaRepository.existsByNombreAndIdNot(nombre, id);
        if (duplicado) {
            throw new ConflictoException("Ya existe otra categoria con ese nombre.");
        }
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoria con ID " + id + "."));
    }

    private Categoria guardar(Categoria entity) {
        return CatalogoSupport.guardar(() -> categoriaRepository.saveAndFlush(entity));
    }
}
