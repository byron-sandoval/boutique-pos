package com.boutique.backend.empresa.mapper;

import com.boutique.backend.empresa.dto.SucursalRequest;
import com.boutique.backend.empresa.dto.SucursalResponse;
import com.boutique.backend.empresa.entity.Empresa;
import com.boutique.backend.empresa.entity.Sucursal;
import org.springframework.stereotype.Component;

@Component
public class SucursalMapper {

    public Sucursal toEntity(SucursalRequest request, Empresa empresa) {
        Sucursal sucursal = new Sucursal();
        updateEntity(request, empresa, sucursal);
        return sucursal;
    }

    // El servicio debe resolver y validar la empresa antes de llamar al mapper.
    public void updateEntity(SucursalRequest request, Empresa empresa, Sucursal sucursal) {
        sucursal.setEmpresa(empresa);
        sucursal.setNombre(request.nombre());
        sucursal.setDireccion(request.direccion());
        sucursal.setTelefono(request.telefono());
        sucursal.setActiva(request.activa());
    }

    public SucursalResponse toResponse(Sucursal sucursal) {
        return new SucursalResponse(
            sucursal.getId(),
            sucursal.getEmpresa().getId(),
            sucursal.getNombre(),
            sucursal.getDireccion(),
            sucursal.getTelefono(),
            sucursal.getActiva()
        );
    }
}
