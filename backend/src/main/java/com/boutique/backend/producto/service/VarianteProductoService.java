package com.boutique.backend.producto.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.boutique.backend.producto.dto.VarianteLoteRequest;
import com.boutique.backend.producto.dto.VarianteProductoRequest;
import com.boutique.backend.producto.dto.VarianteProductoResponse;
import com.boutique.backend.producto.entity.Producto;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.producto.mapper.VarianteProductoMapper;
import com.boutique.backend.producto.repository.ProductoRepository;
import com.boutique.backend.producto.repository.VarianteProductoRepository;
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
public class VarianteProductoService {

    private final VarianteProductoRepository varianteProductoRepository;
    private final ProductoRepository productoRepository;
    private final VarianteProductoMapper varianteProductoMapper;

    public Page<VarianteProductoResponse> listar(@NotNull Pageable pageable) {
        return varianteProductoRepository.findAll(pageable).map(varianteProductoMapper::toResponse);
    }

    public Page<VarianteProductoResponse> listarPorProducto(@NotNull @Positive Long productoId, @NotNull Pageable pageable) {
        buscarProducto(productoId);
        return varianteProductoRepository.findByProductoId(productoId, pageable).map(varianteProductoMapper::toResponse);
    }

    public VarianteProductoResponse obtenerPorId(@NotNull @Positive Long id) {
        return varianteProductoMapper.toResponse(buscarVariante(id));
    }

    @Transactional
    public VarianteProductoResponse crear(@NotNull @Valid VarianteProductoRequest request) {
        Producto producto = buscarProducto(request.productoId());
        VarianteProductoRequest normalizado = normalizar(request);
        validarDuplicados(normalizado, null);
        VarianteProducto entity = varianteProductoMapper.toEntity(normalizado, producto);
        return varianteProductoMapper.toResponse(guardar(entity));
    }

    @Transactional
    public List<VarianteProductoResponse> crearLote(@NotNull @Valid VarianteLoteRequest request) {
        Producto producto = buscarProducto(request.productoId());
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ConflictoException("El producto debe estar activo para registrar el lote.");
        }
        Set<String> skus = new HashSet<>();
        Set<String> codigosBarras = new HashSet<>();
        Set<Combinacion> combinaciones = new HashSet<>();
        List<VarianteProducto> nuevas = new ArrayList<>();
        for (var fila : request.variantes()) {
            var datos = normalizar(new VarianteProductoRequest(producto.getId(),
                fila.talla(), fila.color(), fila.sku(), fila.codigoBarras(), true));
            try {
                if (!skus.add(datos.sku())) {
                    throw new ConflictoException("El SKU está repetido en el lote.");
                }
                if (datos.codigoBarras() != null && !codigosBarras.add(datos.codigoBarras())) {
                    throw new ConflictoException("El código de barras está repetido en el lote.");
                }
                if (!combinaciones.add(new Combinacion(datos.talla(), datos.color()))) {
                    throw new ConflictoException("La talla y el color están repetidos en el lote.");
                }
                validarDuplicados(datos, null);
            } catch (ConflictoException conflicto) {
                throw new ConflictoException("Variante " + datos.sku() + ". " + conflicto.getMessage(), conflicto);
            }
            nuevas.add(varianteProductoMapper.toEntity(datos, producto));
        }
        return CatalogoSupport.guardar(() -> varianteProductoRepository.saveAllAndFlush(nuevas))
            .stream().map(varianteProductoMapper::toResponse).toList();
    }

    private record Combinacion(String talla, String color) {
    }

    @Transactional
    public VarianteProductoResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid VarianteProductoRequest request) {
        VarianteProducto entity = buscarVariante(id);
        if (!entity.getProducto().getId().equals(request.productoId())) {
            throw new ConflictoException("La variante no puede cambiar de producto. Crea una variante nueva.");
        }
        VarianteProductoRequest normalizado = normalizar(request);
        validarDuplicados(normalizado, id);
        varianteProductoMapper.updateEntity(normalizado, entity);
        return varianteProductoMapper.toResponse(guardar(entity));
    }

    @Transactional
    public VarianteProductoResponse cambiarEstado(@NotNull @Positive Long id, boolean activo) {
        VarianteProducto entity = buscarVariante(id);
        entity.setActivo(activo);
        return varianteProductoMapper.toResponse(guardar(entity));
    }

    private VarianteProductoRequest normalizar(VarianteProductoRequest request) {
        return new VarianteProductoRequest(
            request.productoId(),
            CatalogoSupport.textoOpcional(request.talla()),
            CatalogoSupport.textoOpcional(request.color()),
            request.sku().trim(),
            CatalogoSupport.textoOpcional(request.codigoBarras()),
            request.activo()
        );
    }

    private void validarDuplicados(VarianteProductoRequest request, Long id) {
        boolean skuDuplicado = id == null
            ? varianteProductoRepository.existsBySku(request.sku())
            : varianteProductoRepository.existsBySkuAndIdNot(request.sku(), id);
        if (skuDuplicado) {
            throw new ConflictoException("Ya existe otra variante con ese SKU.");
        }
        if (request.codigoBarras() != null) {
            boolean barrasDuplicado = id == null
                ? varianteProductoRepository.existsByCodigoBarras(request.codigoBarras())
                : varianteProductoRepository.existsByCodigoBarrasAndIdNot(request.codigoBarras(), id);
            if (barrasDuplicado) {
                throw new ConflictoException("Ya existe otra variante con ese codigo de barras.");
            }
        }
        // Spring Data compara con IS NULL los parametros nulos, como exige NULLS NOT DISTINCT.
        boolean combinacionDuplicada = id == null
            ? varianteProductoRepository.existsByProductoIdAndTallaAndColor(
                request.productoId(), request.talla(), request.color())
            : varianteProductoRepository.existsByProductoIdAndTallaAndColorAndIdNot(
                request.productoId(), request.talla(), request.color(), id);
        if (combinacionDuplicada) {
            throw new ConflictoException("Ya existe esa combinacion de producto, talla y color.");
        }
    }

    private VarianteProducto buscarVariante(Long id) {
        return varianteProductoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la variante con ID " + id + "."));
    }

    private Producto buscarProducto(Long id) {
        return productoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con ID " + id + "."));
    }

    private VarianteProducto guardar(VarianteProducto entity) {
        return CatalogoSupport.guardar(() -> varianteProductoRepository.saveAndFlush(entity));
    }
}
