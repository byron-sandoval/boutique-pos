package com.boutique.backend.producto.mapper;

import com.boutique.backend.producto.dto.ProductoRequest;
import com.boutique.backend.producto.dto.ProductoResponse;
import com.boutique.backend.producto.entity.Producto;
import com.boutique.backend.producto.entity.Categoria;
import com.boutique.backend.producto.entity.UnidadMedida;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public Producto toEntity(ProductoRequest request, Categoria categoria, UnidadMedida unidadMedida) {
        Producto entity = new Producto();
        updateEntity(request, categoria, unidadMedida, entity);
        return entity;
    }

    public void updateEntity(ProductoRequest request, Categoria categoria, UnidadMedida unidadMedida, Producto entity) {
        entity.setCategoria(categoria);
        entity.setUnidadMedida(unidadMedida);
        entity.setCodigo(request.codigo());
        entity.setNombre(request.nombre());
        entity.setDescripcion(request.descripcion());
        entity.setMarca(request.marca());
        entity.setPrecio(request.precio());
        entity.setCosto(request.costo());
        entity.setUltimoCosto(request.ultimoCosto());
        entity.setImagenUrl(request.imagenUrl());
        entity.setActivo(request.activo());
    }

    public ProductoResponse toResponse(Producto entity) {
        return new ProductoResponse(
            entity.getId(),
            entity.getCategoria().getId(),
            entity.getUnidadMedida().getId(),
            entity.getCodigo(),
            entity.getNombre(),
            entity.getDescripcion(),
            entity.getMarca(),
            entity.getPrecio(),
            entity.getCosto(),
            entity.getUltimoCosto(),
            entity.getImagenUrl(),
            entity.getActivo()
        );
    }
}
