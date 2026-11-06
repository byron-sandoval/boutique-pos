package com.boutique.backend.caja.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.boutique.backend.caja.entity.EstadoCaja;

public record SesionCajaResponse(
    Long id,
    Long sucursalId,
    Long abiertoPorId,
    Long cerradoPorId,
    EstadoCaja estado,
    Instant abiertoEn,
    Instant cerradoEn,
    BigDecimal efectivoInicial,
    BigDecimal efectivoCierre,
    BigDecimal efectivoEsperado,
    BigDecimal diferencia,
    BigDecimal totalVentas,
    BigDecimal totalEfectivo,
    BigDecimal totalTarjeta,
    BigDecimal totalTransferencia,
    Integer cantidadVentas,
    String notas
) {
}
