package com.boutique.backend.proveedor.mapper;

import com.boutique.backend.proveedor.dto.ProveedorRequest;
import com.boutique.backend.proveedor.dto.ProveedorResponse;
import com.boutique.backend.proveedor.entity.Proveedor;
import org.springframework.stereotype.Component;

@Component
public class ProveedorMapper {

    public Proveedor toEntity(ProveedorRequest request) {
        Proveedor proveedor = new Proveedor();
        updateEntity(request, proveedor);
        return proveedor;
    }

    public void updateEntity(ProveedorRequest request, Proveedor proveedor) {
        proveedor.setNombre(request.nombre());
        proveedor.setNombreContacto(request.nombreContacto());
        proveedor.setDireccion(request.direccion());
        proveedor.setTelefono(request.telefono());
        proveedor.setEmail(request.email());
        proveedor.setActivo(request.activo());
    }

    public ProveedorResponse toResponse(Proveedor proveedor) {
        return new ProveedorResponse(
            proveedor.getId(),
            proveedor.getNombre(),
            proveedor.getNombreContacto(),
            proveedor.getDireccion(),
            proveedor.getTelefono(),
            proveedor.getEmail(),
            proveedor.getActivo()
        );
    }
}
