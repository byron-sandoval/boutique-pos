package com.boutique.backend.usuario.dto;

import jakarta.validation.constraints.NotNull;

public record UsuarioEstadoRequest(@NotNull Boolean activo) {
}
