package com.boutique.backend.empresa.dto;

import java.math.BigDecimal;

public record EmpresaResponse(
    Long id,
    String nombre,
    String ruc,
    String regimenFiscal,
    String direccion,
    String telefono,
    String correo,
    String logoUrl,
    String eslogan,
    BigDecimal porcentajeIva
) {
}
