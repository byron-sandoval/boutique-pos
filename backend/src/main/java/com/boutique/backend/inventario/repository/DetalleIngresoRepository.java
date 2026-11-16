package com.boutique.backend.inventario.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.boutique.backend.inventario.entity.DetalleIngreso;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DetalleIngresoRepository extends JpaRepository<DetalleIngreso, Long> {

    @EntityGraph(attributePaths = {"variante", "variante.producto"})
    List<DetalleIngreso> findByIngresoIdOrderByIdAsc(Long ingresoId);

    @Query(value = """
        SELECT d.costo_unitario
        FROM detalle_ingreso d
        JOIN ingreso i ON i.id = d.ingreso_id
        WHERE d.variante_id = :varianteId AND i.sucursal_id = :sucursalId AND i.activo = true
        ORDER BY i.fecha DESC, i.id DESC, d.id DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<BigDecimal> buscarUltimoCosto(Long varianteId, Long sucursalId);
}
