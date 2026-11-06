package com.boutique.backend.producto.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoCatalogoRequest(@NotNull Boolean activo) {
}
