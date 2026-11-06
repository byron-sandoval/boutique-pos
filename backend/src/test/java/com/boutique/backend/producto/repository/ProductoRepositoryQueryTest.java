package com.boutique.backend.producto.repository;

import java.util.stream.Stream;

import com.boutique.backend.producto.entity.Categoria;
import com.boutique.backend.producto.entity.Producto;
import com.boutique.backend.producto.entity.UnidadMedida;
import com.boutique.backend.producto.entity.VarianteProducto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.repository.query.parser.PartTree;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProductoRepositoryQueryTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("consultas")
    void lasConsultasDerivadasUsanPropiedadesExistentes(String metodo, Class<?> entity) {
        assertDoesNotThrow(() -> new PartTree(metodo, entity));
    }

    private static Stream<Arguments> consultas() {
        return Stream.of(
            consultas(CategoriaRepository.class, Categoria.class),
            consultas(UnidadMedidaRepository.class, UnidadMedida.class),
            consultas(ProductoRepository.class, Producto.class),
            consultas(VarianteProductoRepository.class, VarianteProducto.class)
        ).flatMap(stream -> stream);
    }

    private static Stream<Arguments> consultas(Class<?> repository, Class<?> entity) {
        return Stream.of(repository.getDeclaredMethods()).map(method -> Arguments.of(method.getName(), entity));
    }
}
