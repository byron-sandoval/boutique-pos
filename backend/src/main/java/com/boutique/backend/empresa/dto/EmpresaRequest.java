package com.boutique.backend.empresa.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmpresaRequest(
    @NotBlank @Size(max = 100) String nombre,
    @Size(max = 20) String ruc,
    @Size(max = 100) String regimenFiscal,
    @NotBlank @Size(max = 255) String direccion,
    @Size(max = 20) String telefono,
    @Email @Size(max = 100) String correo,
    @Size(max = 500) String logoUrl,
    @Size(max = 150) String eslogan,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2) BigDecimal porcentajeIva
) {
}
