package com.boutique.backend.producto.service;

import com.boutique.backend.producto.dto.ProductoRequest;
import com.boutique.backend.producto.dto.ProductoResponse;
import com.boutique.backend.producto.entity.Categoria;
import com.boutique.backend.producto.entity.Producto;
import com.boutique.backend.producto.entity.UnidadMedida;
import com.boutique.backend.producto.mapper.ProductoMapper;
import com.boutique.backend.producto.repository.CategoriaRepository;
import com.boutique.backend.producto.repository.ProductoRepository;
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
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoMapper productoMapper;

    public Page<ProductoResponse> listar(@NotNull Pageable pageable) {
        return productoRepository.findAll(pageable).map(productoMapper::toResponse);
    }

    public Page<ProductoResponse> listarPorCategoria(@NotNull @Positive Long categoriaId, @NotNull Pageable pageable) {
        buscarCategoria(categoriaId);
        return productoRepository.findByCategoriaId(categoriaId, pageable).map(productoMapper::toResponse);
    }

    public ProductoResponse obtenerPorId(@NotNull @Positive Long id) {
        return productoMapper.toResponse(buscarProducto(id));
    }

    @Transactional
    public ProductoResponse crear(@NotNull @Valid ProductoRequest request) {
        Categoria categoria = buscarCategoria(request.categoriaId());
        UnidadMedida unidad = buscarUnidad(request.unidadMedidaId());
        String codigo = request.codigo().trim();
        validarCodigo(codigo, null);
        Producto entity = productoMapper.toEntity(request, categoria, unidad);
        entity.setCodigo(codigo);
        return productoMapper.toResponse(guardar(entity));
    }

    @Transactional
    public ProductoResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid ProductoRequest request) {
        Producto entity = buscarProducto(id);
        Categoria categoria = buscarCategoria(request.categoriaId());
        UnidadMedida unidad = buscarUnidad(request.unidadMedidaId());
        String codigo = request.codigo().trim();
        validarCodigo(codigo, id);
        productoMapper.updateEntity(request, categoria, unidad, entity);
        entity.setCodigo(codigo);
        return productoMapper.toResponse(guardar(entity));
    }

    @Transactional
    public ProductoResponse cambiarEstado(@NotNull @Positive Long id, boolean activo) {
        Producto entity = buscarProducto(id);
        entity.setActivo(activo);
        return productoMapper.toResponse(guardar(entity));
    }

    private void validarCodigo(String codigo, Long id) {
        boolean duplicado = id == null
            ? productoRepository.existsByCodigo(codigo)
            : productoRepository.existsByCodigoAndIdNot(codigo, id);
        if (duplicado) {
            throw new ConflictoException("Ya existe otro producto con ese codigo.");
        }
    }

    private Producto buscarProducto(Long id) {
        return productoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con ID " + id + "."));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoria con ID " + id + "."));
    }

    private UnidadMedida buscarUnidad(Long id) {
        return unidadMedidaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la unidad de medida con ID " + id + "."));
    }

    private Producto guardar(Producto entity) {
        return CatalogoSupport.guardar(() -> productoRepository.saveAndFlush(entity));
    }
}
