package com.boutique.backend.venta.repository;

import java.math.BigDecimal;

import com.boutique.backend.venta.entity.Devolucion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {
    boolean existsByVentaId(Long ventaId);

    @Query("""
        select d from Devolucion d
        where (:ventaId is null or d.venta.id = :ventaId)
          and (:sucursalId is null or d.venta.sucursal.id = :sucursalId)
        """)
    Page<Devolucion> buscar(@Param("ventaId") Long ventaId, @Param("sucursalId") Long sucursalId, Pageable pageable);

    @Query("select coalesce(sum(d.total), 0) from Devolucion d where d.venta.id = :ventaId")
    BigDecimal totalDevuelto(@Param("ventaId") Long ventaId);
}
