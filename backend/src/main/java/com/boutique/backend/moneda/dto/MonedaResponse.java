package com.boutique.backend.moneda.dto;

public record MonedaResponse(Long id, String codigo, String nombre, String simbolo, Boolean activo) {
}
