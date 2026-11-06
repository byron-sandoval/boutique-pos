package com.boutique.backend.moneda.dto;

import jakarta.validation.constraints.NotNull;

public record MonedaEstadoRequest(@NotNull Boolean activo) {
}
