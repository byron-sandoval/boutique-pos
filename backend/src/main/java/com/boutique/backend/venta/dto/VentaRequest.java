package com.boutique.backend.venta.dto;

import java.math.BigDecimal;
import java.util.List;

import com.boutique.backend.venta.entity.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VentaRequest(
    @NotNull @Positive Long sesionCajaId,
    @Positive Long clienteId,
    @NotNull @Positive Long monedaId,
    @NotNull @Positive Long serieFacturaId,
    @NotNull MetodoPago metodoPago,
    @NotNull @DecimalMin("0.0001") @Digits(integer = 8, fraction = 4) BigDecimal tipoCambioAplicado,
    @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal importeRecibido,
    @NotEmpty @Size(max = 500) List<@NotNull @Valid DetalleVentaRequest> detalles
) {
}
