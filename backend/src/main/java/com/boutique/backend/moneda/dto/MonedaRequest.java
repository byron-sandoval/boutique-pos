package com.boutique.backend.moneda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MonedaRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "debe tener exactamente tres letras") String codigo,
    @NotBlank @Size(max = 50) String nombre,
    @NotBlank @Size(max = 5) String simbolo,
    @NotNull Boolean activo
) {
}
