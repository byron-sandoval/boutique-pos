package com.boutique.backend.empresa.mapper;

import com.boutique.backend.empresa.dto.EmpresaRequest;
import com.boutique.backend.empresa.dto.EmpresaResponse;
import com.boutique.backend.empresa.entity.Empresa;
import org.springframework.stereotype.Component;

@Component
public class EmpresaMapper {

    public Empresa toEntity(EmpresaRequest request) {
        Empresa empresa = new Empresa();
        updateEntity(request, empresa);
        return empresa;
    }

    public void updateEntity(EmpresaRequest request, Empresa empresa) {
        empresa.setNombre(request.nombre());
        empresa.setRuc(request.ruc());
        empresa.setRegimenFiscal(request.regimenFiscal());
        empresa.setDireccion(request.direccion());
        empresa.setTelefono(request.telefono());
        empresa.setCorreo(request.correo());
        empresa.setLogoUrl(request.logoUrl());
        empresa.setEslogan(request.eslogan());
        empresa.setPorcentajeIva(request.porcentajeIva());
    }

    public EmpresaResponse toResponse(Empresa empresa) {
        return new EmpresaResponse(
            empresa.getId(),
            empresa.getNombre(),
            empresa.getRuc(),
            empresa.getRegimenFiscal(),
            empresa.getDireccion(),
            empresa.getTelefono(),
            empresa.getCorreo(),
            empresa.getLogoUrl(),
            empresa.getEslogan(),
            empresa.getPorcentajeIva()
        );
    }
}
