package com.boutique.backend.venta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VentaAnulacionRequest(@NotBlank @Size(max = 255) String motivo) {
}
