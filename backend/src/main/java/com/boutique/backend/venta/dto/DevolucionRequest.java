package com.boutique.backend.venta.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DevolucionRequest(
    @NotNull @Positive Long ventaId,
    @NotBlank @Size(max = 255) String motivo,
    @NotEmpty @Size(max = 500) List<@NotNull @Valid DetalleDevolucionRequest> detalles
) {
}
