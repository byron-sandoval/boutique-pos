package com.boutique.backend.cliente.mapper;

import com.boutique.backend.cliente.dto.ClienteRequest;
import com.boutique.backend.cliente.dto.ClienteResponse;
import com.boutique.backend.cliente.entity.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public Cliente toEntity(ClienteRequest request) {
        Cliente cliente = new Cliente();
        updateEntity(request, cliente);
        return cliente;
    }

    public void updateEntity(ClienteRequest request, Cliente cliente) {
        cliente.setCedula(request.cedula());
        cliente.setPasaporte(request.pasaporte());
        cliente.setNombre(request.nombre());
        cliente.setDireccion(request.direccion());
        cliente.setTelefono(request.telefono());
        cliente.setActivo(request.activo());
    }

    public ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
            cliente.getId(),
            cliente.getCedula(),
            cliente.getPasaporte(),
            cliente.getNombre(),
            cliente.getDireccion(),
            cliente.getTelefono(),
            cliente.getActivo()
        );
    }
}
