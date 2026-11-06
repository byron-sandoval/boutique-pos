package com.boutique.backend.empresa.dto;

import jakarta.validation.constraints.NotNull;

public record SucursalEstadoRequest(@NotNull Boolean activa) {
}
