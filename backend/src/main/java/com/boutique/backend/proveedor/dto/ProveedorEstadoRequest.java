package com.boutique.backend.proveedor.dto;

import jakarta.validation.constraints.NotNull;

public record ProveedorEstadoRequest(@NotNull Boolean activo) {
}
