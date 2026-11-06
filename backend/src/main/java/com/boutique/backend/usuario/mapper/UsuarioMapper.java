package com.boutique.backend.usuario.mapper;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.usuario.dto.UsuarioCreateRequest;
import com.boutique.backend.usuario.dto.UsuarioResponse;
import com.boutique.backend.usuario.dto.UsuarioUpdateRequest;
import com.boutique.backend.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toEntity(UsuarioCreateRequest request, Sucursal sucursal, String keycloakId) {
        Usuario usuario = new Usuario();
        usuario.setKeycloakId(keycloakId);
        usuario.setUsername(request.username());
        usuario.setActivo(request.activo());
        updateEntity(new UsuarioUpdateRequest(request.sucursalId(), request.cedula(), request.nombre(),
            request.apellido(), request.telefono(), request.email()), sucursal, usuario);
        return usuario;
    }

    public void updateEntity(UsuarioUpdateRequest request, Sucursal sucursal, Usuario usuario) {
        usuario.setSucursal(sucursal);
        usuario.setCedula(request.cedula());
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setTelefono(request.telefono());
        usuario.setEmail(request.email());
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getKeycloakId(), usuario.getSucursal().getId(),
            usuario.getCedula(), usuario.getNombre(), usuario.getApellido(), usuario.getTelefono(),
            usuario.getEmail(), usuario.getUsername(), usuario.getActivo());
    }
}
