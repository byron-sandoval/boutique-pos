package com.boutique.backend.venta.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SerieFacturaUpdateRequest(
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9_-]+") String prefijo,
    @NotNull @Min(1) @Max(15) Integer digitos,
    @NotNull Boolean activa
) {
}
