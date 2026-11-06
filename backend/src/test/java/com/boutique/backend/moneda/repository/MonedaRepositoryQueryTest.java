package com.boutique.backend.moneda.repository;

import java.util.stream.Stream;

import com.boutique.backend.moneda.entity.Moneda;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.parser.PartTree;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MonedaRepositoryQueryTest {
    @ParameterizedTest
    @MethodSource("consultas")
    void consultasDerivadasUsanPropiedadesExistentes(String metodo) {
        assertDoesNotThrow(() -> new PartTree(metodo, Moneda.class));
    }

    static Stream<String> consultas() {
        return Stream.of(MonedaRepository.class.getDeclaredMethods())
            .filter(method -> !method.isAnnotationPresent(Query.class)).map(method -> method.getName());
    }
}
