package com.boutique.backend.venta.repository;

import java.util.stream.Stream;

import com.boutique.backend.venta.entity.DetalleVenta;
import com.boutique.backend.venta.entity.Devolucion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.repository.query.parser.PartTree;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class VentaRepositoryQueryTest {
    @Test
    void consultaDeDevolucionesUsaUnaRelacionExistente() {
        assertDoesNotThrow(() -> new PartTree("existsByVentaId", Devolucion.class));
    }

    @ParameterizedTest
    @MethodSource("consultas")
    void consultasDeDetallesUsanPropiedadesExistentes(String metodo) {
        assertDoesNotThrow(() -> new PartTree(metodo, DetalleVenta.class));
    }

    static Stream<String> consultas() {
        return Stream.of(DetalleVentaRepository.class.getDeclaredMethods()).map(method -> method.getName());
    }
}
