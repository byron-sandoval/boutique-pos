package com.boutique.backend.cliente.dto;

import jakarta.validation.constraints.NotNull;

public record ClienteEstadoRequest(@NotNull Boolean activo) {
}
