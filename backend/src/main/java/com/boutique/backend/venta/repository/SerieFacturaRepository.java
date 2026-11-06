package com.boutique.backend.venta.repository;

import java.util.Optional;

import com.boutique.backend.venta.entity.SerieFactura;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerieFacturaRepository extends JpaRepository<SerieFactura, Long> {

    boolean existsBySucursalIdAndPrefijo(Long sucursalId, String prefijo);

    boolean existsBySucursalIdAndPrefijoAndIdNot(Long sucursalId, String prefijo, Long id);

    @Query("""
        select s from SerieFactura s
        where (:sucursalId is null or s.sucursal.id = :sucursalId)
          and (:activa is null or s.activa = :activa)
        """)
    Page<SerieFactura> buscar(@Param("sucursalId") Long sucursalId, @Param("activa") Boolean activa, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SerieFactura s where s.id = :id")
    Optional<SerieFactura> findByIdForUpdate(@Param("id") Long id);

    @Query("select count(v) from Venta v where v.serieFactura.id = :serieId")
    long contarVentas(@Param("serieId") Long serieId);
}
