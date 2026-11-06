package com.boutique.backend.moneda.mapper;

import com.boutique.backend.moneda.dto.MonedaRequest;
import com.boutique.backend.moneda.dto.MonedaResponse;
import com.boutique.backend.moneda.entity.Moneda;
import org.springframework.stereotype.Component;

@Component
public class MonedaMapper {

    public Moneda toEntity(MonedaRequest request) {
        Moneda moneda = new Moneda();
        updateEntity(request, moneda);
        return moneda;
    }

    public void updateEntity(MonedaRequest request, Moneda moneda) {
        moneda.setCodigo(request.codigo());
        moneda.setNombre(request.nombre());
        moneda.setSimbolo(request.simbolo());
        moneda.setActivo(request.activo());
    }

    public MonedaResponse toResponse(Moneda moneda) {
        return new MonedaResponse(moneda.getId(), moneda.getCodigo(), moneda.getNombre(),
            moneda.getSimbolo(), moneda.getActivo());
    }
}
