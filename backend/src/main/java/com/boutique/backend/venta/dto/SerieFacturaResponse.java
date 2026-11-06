package com.boutique.backend.venta.dto;

public record SerieFacturaResponse(Long id, Long sucursalId, String prefijo, Integer digitos,
    Long siguienteNumero, Boolean activa) {
}
