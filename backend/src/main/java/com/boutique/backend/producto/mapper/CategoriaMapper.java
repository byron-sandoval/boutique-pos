package com.boutique.backend.producto.mapper;

import com.boutique.backend.producto.dto.CategoriaRequest;
import com.boutique.backend.producto.dto.CategoriaResponse;
import com.boutique.backend.producto.entity.Categoria;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

    public Categoria toEntity(CategoriaRequest request) {
        Categoria entity = new Categoria();
        updateEntity(request, entity);
        return entity;
    }

    public void updateEntity(CategoriaRequest request, Categoria entity) {
        entity.setNombre(request.nombre());
        entity.setDescripcion(request.descripcion());
        entity.setActivo(request.activo());
    }

    public CategoriaResponse toResponse(Categoria entity) {
        return new CategoriaResponse(
            entity.getId(),
            entity.getNombre(),
            entity.getDescripcion(),
            entity.getActivo()
        );
    }
}
