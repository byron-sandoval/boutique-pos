package com.boutique.backend.caja.repository;

import java.util.stream.Stream;

import com.boutique.backend.caja.entity.SesionCaja;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.parser.PartTree;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SesionCajaRepositoryQueryTest {
    @ParameterizedTest
    @MethodSource("consultas")
    void consultasDerivadasUsanPropiedadesExistentes(String metodo) {
        assertDoesNotThrow(() -> new PartTree(metodo, SesionCaja.class));
    }

    static Stream<String> consultas() {
        return Stream.of(SesionCajaRepository.class.getDeclaredMethods())
            .filter(method -> !method.isAnnotationPresent(Query.class)).map(method -> method.getName());
    }
}
