package com.boutique.backend.producto.mapper;

import com.boutique.backend.producto.dto.UnidadMedidaRequest;
import com.boutique.backend.producto.dto.UnidadMedidaResponse;
import com.boutique.backend.producto.entity.UnidadMedida;
import org.springframework.stereotype.Component;

@Component
public class UnidadMedidaMapper {

    public UnidadMedida toEntity(UnidadMedidaRequest request) {
        UnidadMedida entity = new UnidadMedida();
        updateEntity(request, entity);
        return entity;
    }

    public void updateEntity(UnidadMedidaRequest request, UnidadMedida entity) {
        entity.setNombre(request.nombre());
        entity.setSimbolo(request.simbolo());
        entity.setActivo(request.activo());
    }

    public UnidadMedidaResponse toResponse(UnidadMedida entity) {
        return new UnidadMedidaResponse(
            entity.getId(),
            entity.getNombre(),
            entity.getSimbolo(),
            entity.getActivo()
        );
    }
}
