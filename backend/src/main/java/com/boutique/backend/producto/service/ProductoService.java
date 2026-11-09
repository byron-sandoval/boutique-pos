package com.boutique.backend.producto.service;

import com.boutique.backend.empresa.service.LogoStorage;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoMapper productoMapper;
    private final LogoStorage imagenStorage;

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
        return crear(request, null);
    }

    @Transactional
    public ProductoResponse crear(@NotNull @Valid ProductoRequest request, MultipartFile imagen) {
        Categoria categoria = buscarCategoria(request.categoriaId());
        UnidadMedida unidad = buscarUnidad(request.unidadMedidaId());
        String codigo = request.codigo().trim();
        validarCodigo(codigo, null);
        Producto entity = productoMapper.toEntity(request, categoria, unidad);
        entity.setCodigo(codigo);
        if (imagen != null) {
            entity.setImagenUrl(imagenStorage.subirProducto(imagen, null));
        }
        return productoMapper.toResponse(guardar(entity));
    }

    @Transactional
    public ProductoResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid ProductoRequest request) {
        return actualizar(id, request, null, false);
    }

    @Transactional
    public ProductoResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid ProductoRequest request, MultipartFile imagen) {
        return actualizar(id, request, imagen, true);
    }

    private ProductoResponse actualizar(Long id, ProductoRequest request, MultipartFile imagen, boolean conservarImagen) {
        Producto entity = bloquearProducto(id);
        Categoria categoria = buscarCategoria(request.categoriaId());
        UnidadMedida unidad = buscarUnidad(request.unidadMedidaId());
        String codigo = request.codigo().trim();
        validarCodigo(codigo, id);
        String imagenUrl = entity.getImagenUrl();
        if (imagen != null) {
            boolean compartida = imagenUrl != null && productoRepository.existsByImagenUrlAndIdNot(imagenUrl, id);
            imagenUrl = imagenStorage.subirProducto(imagen, compartida ? null : imagenUrl);
        }
        productoMapper.updateEntity(request, categoria, unidad, entity);
        entity.setCodigo(codigo);
        if (conservarImagen) {
            entity.setImagenUrl(imagenUrl);
        }
        return productoMapper.toResponse(guardar(entity));
    }

    @Transactional
    public ProductoResponse cambiarEstado(@NotNull @Positive Long id, boolean activo) {
        Producto entity = bloquearProducto(id);
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
            .orElseThrow(() -> productoNoEncontrado(id));
    }

    private Producto bloquearProducto(Long id) {
        return productoRepository.findByIdForUpdate(id).orElseThrow(() -> productoNoEncontrado(id));
    }

    private RecursoNoEncontradoException productoNoEncontrado(Long id) {
        return new RecursoNoEncontradoException("No existe el producto con ID " + id + ".");
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
