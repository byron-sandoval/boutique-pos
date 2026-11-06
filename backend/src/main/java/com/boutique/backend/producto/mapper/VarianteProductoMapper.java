package com.boutique.backend.producto.mapper;

import com.boutique.backend.producto.dto.VarianteProductoRequest;
import com.boutique.backend.producto.dto.VarianteProductoResponse;
import com.boutique.backend.producto.entity.VarianteProducto;
import com.boutique.backend.producto.entity.Producto;
import org.springframework.stereotype.Component;

@Component
public class VarianteProductoMapper {

    public VarianteProducto toEntity(VarianteProductoRequest request, Producto producto) {
        VarianteProducto entity = new VarianteProducto();
        entity.setProducto(producto);
        updateEntity(request, entity);
        return entity;
    }

    public void updateEntity(VarianteProductoRequest request, VarianteProducto entity) {
        entity.setTalla(request.talla());
        entity.setColor(request.color());
        entity.setSku(request.sku());
        entity.setCodigoBarras(request.codigoBarras());
        entity.setActivo(request.activo());
    }

    public VarianteProductoResponse toResponse(VarianteProducto entity) {
        return new VarianteProductoResponse(
            entity.getId(),
            entity.getProducto().getId(),
            entity.getTalla(),
            entity.getColor(),
            entity.getSku(),
            entity.getCodigoBarras(),
            entity.getActivo()
        );
    }
}
