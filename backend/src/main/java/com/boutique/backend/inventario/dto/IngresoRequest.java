package com.boutique.backend.inventario.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record IngresoRequest(
    @NotNull @Positive Long sucursalId,
    @Positive Long proveedorId,
    @NotBlank @Size(max = 50) String noDocumento,
    @Size(max = 255) String observaciones,
    @NotEmpty @Size(max = 500) List<@NotNull @Valid DetalleIngresoRequest> detalles
) {
}
