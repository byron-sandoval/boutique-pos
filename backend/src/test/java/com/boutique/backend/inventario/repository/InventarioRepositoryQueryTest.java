package com.boutique.backend.inventario.repository;

import java.util.stream.Stream;

import com.boutique.backend.inventario.entity.DetalleIngreso;
import com.boutique.backend.inventario.entity.Ingreso;
import com.boutique.backend.inventario.entity.Inventario;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.parser.PartTree;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InventarioRepositoryQueryTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("consultas")
    void consultasDerivadasUsanPropiedadesExistentes(String metodo, Class<?> entidad) {
        assertDoesNotThrow(() -> new PartTree(metodo, entidad));
    }

    static Stream<Arguments> consultas() {
        return Stream.of(consultas(InventarioRepository.class, Inventario.class),
            consultas(IngresoRepository.class, Ingreso.class),
            consultas(DetalleIngresoRepository.class, DetalleIngreso.class)).flatMap(stream -> stream);
    }

    private static Stream<Arguments> consultas(Class<?> repository, Class<?> entidad) {
        return Stream.of(repository.getDeclaredMethods())
            .filter(method -> !method.isAnnotationPresent(Query.class))
            .map(method -> Arguments.of(method.getName(), entidad));
    }
}
