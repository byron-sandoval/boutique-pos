package com.boutique.backend.inventario.repository;

import java.util.Optional;

import com.boutique.backend.inventario.entity.Inventario;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    boolean existsByVarianteIdAndSucursalId(Long varianteId, Long sucursalId);

    Page<Inventario> findBySucursalId(Long sucursalId, Pageable pageable);

    Page<Inventario> findByVarianteId(Long varianteId, Pageable pageable);

    Page<Inventario> findBySucursalIdAndVarianteId(Long sucursalId, Long varianteId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventario i where i.id = :id")
    Optional<Inventario> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query(value = """
        INSERT INTO inventario (variante_id, sucursal_id, stock, stock_minimo)
        VALUES (:varianteId, :sucursalId, :cantidad, 0)
        ON CONFLICT ON CONSTRAINT uq_inventario_variante_sucursal
        DO UPDATE SET stock = inventario.stock + EXCLUDED.stock
        WHERE inventario.stock <= 2147483647 - EXCLUDED.stock
        """, nativeQuery = true)
    int sumarStock(@Param("varianteId") Long varianteId, @Param("sucursalId") Long sucursalId,
        @Param("cantidad") int cantidad);

    @Modifying(flushAutomatically = true)
    @Query(value = """
        UPDATE inventario SET stock = stock - :cantidad
        WHERE variante_id = :varianteId AND sucursal_id = :sucursalId AND stock >= :cantidad
        """, nativeQuery = true)
    int restarStock(@Param("varianteId") Long varianteId, @Param("sucursalId") Long sucursalId,
        @Param("cantidad") int cantidad);
}
