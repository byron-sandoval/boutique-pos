package com.boutique.backend.moneda.repository;

import java.util.Optional;

import com.boutique.backend.moneda.entity.Moneda;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonedaRepository extends JpaRepository<Moneda, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    Page<Moneda> findByActivo(boolean activo, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Moneda m where m.id = :id")
    Optional<Moneda> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select m from Moneda m where m.id = :id")
    Optional<Moneda> findByIdParaVenta(@Param("id") Long id);

    @Query("select count(v) from Venta v where v.moneda.id = :monedaId")
    long contarVentas(@Param("monedaId") Long monedaId);
}
