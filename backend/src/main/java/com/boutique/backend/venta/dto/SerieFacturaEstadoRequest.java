package com.boutique.backend.venta.dto;

import jakarta.validation.constraints.NotNull;

public record SerieFacturaEstadoRequest(@NotNull Boolean activa) {
}
