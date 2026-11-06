package com.boutique.backend.caja.repository;

import java.util.Optional;

import com.boutique.backend.caja.entity.EstadoCaja;
import com.boutique.backend.caja.entity.SesionCaja;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {

    boolean existsByAbiertoPorIdAndEstado(Long usuarioId, EstadoCaja estado);

    Optional<SesionCaja> findByAbiertoPorIdAndSucursalIdAndEstado(Long usuarioId, Long sucursalId, EstadoCaja estado);

    @Query("""
        select c from SesionCaja c
        where c.sucursal.id = :sucursalId
          and (:usuarioId is null or c.abiertoPor.id = :usuarioId)
          and (:estado is null or c.estado = :estado)
        """)
    Page<SesionCaja> buscar(@Param("sucursalId") Long sucursalId, @Param("usuarioId") Long usuarioId,
        @Param("estado") EstadoCaja estado, Pageable pageable);

    @Query("""
        select c from SesionCaja c
        where c.id = :id and c.sucursal.id = :sucursalId
          and (:usuarioId is null or c.abiertoPor.id = :usuarioId)
        """)
    Optional<SesionCaja> findVisibleById(@Param("id") Long id, @Param("sucursalId") Long sucursalId,
        @Param("usuarioId") Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SesionCaja c where c.id = :id")
    Optional<SesionCaja> findByIdForUpdate(@Param("id") Long id);
}
