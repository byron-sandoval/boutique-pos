package com.boutique.backend.producto.repository;

import java.util.List;

import com.boutique.backend.producto.entity.VarianteProducto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface VarianteProductoRepository extends JpaRepository<VarianteProducto, Long> {

    @Override
    @EntityGraph(attributePaths = "producto")
    List<VarianteProducto> findAllById(Iterable<Long> ids);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);

    boolean existsByProductoIdAndTallaAndColor(Long productoId, String talla, String color);

    boolean existsByProductoIdAndTallaAndColorAndIdNot(Long productoId, String talla, String color, Long id);

    Page<VarianteProducto> findByProductoId(Long productoId, Pageable pageable);
}
