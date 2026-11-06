package com.boutique.backend.producto.service;

import java.util.Map;
import java.util.function.Supplier;

import com.boutique.backend.shared.exception.ConflictoException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

final class CatalogoSupport {

    private static final Map<String, String> CONFLICTOS = Map.of(
        "categoria_nombre_key", "Ya existe una categoria con ese nombre.",
        "unidad_medida_nombre_key", "Ya existe una unidad de medida con ese nombre.",
        "producto_codigo_key", "Ya existe un producto con ese codigo.",
        "variante_producto_sku_key", "Ya existe una variante con ese SKU.",
        "variante_producto_codigo_barras_key", "Ya existe una variante con ese codigo de barras.",
        "uq_variante_producto", "Ya existe esa combinacion de producto, talla y color."
    );

    private CatalogoSupport() {
    }

    static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    static <T> T guardar(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (DataIntegrityViolationException exception) {
            // La base sigue protegiendo la unicidad ante escrituras simultaneas.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation) {
                    String constraint = violation.getConstraintName();
                    String message = constraint == null ? null : CONFLICTOS.get(constraint);
                    if (message != null) {
                        throw new ConflictoException(message, exception);
                    }
                }
            }
            throw exception;
        }
    }
}
