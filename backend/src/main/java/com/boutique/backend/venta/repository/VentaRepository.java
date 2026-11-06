package com.boutique.backend.venta.repository;

import java.util.Optional;

import com.boutique.backend.venta.entity.EstadoVenta;
import com.boutique.backend.venta.entity.Venta;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("""
        select v from Venta v
        where v.sucursal.id = :sucursalId
          and (:usuarioId is null or v.usuario.id = :usuarioId)
          and (:sesionCajaId is null or v.sesionCaja.id = :sesionCajaId)
          and (:estado is null or v.estado = :estado)
        """)
    Page<Venta> buscar(@Param("sucursalId") Long sucursalId, @Param("usuarioId") Long usuarioId,
        @Param("sesionCajaId") Long sesionCajaId, @Param("estado") EstadoVenta estado, Pageable pageable);

    @Query("""
        select v from Venta v
        where v.id = :id and v.sucursal.id = :sucursalId
          and (:usuarioId is null or v.usuario.id = :usuarioId)
        """)
    Optional<Venta> findVisibleById(@Param("id") Long id, @Param("sucursalId") Long sucursalId,
        @Param("usuarioId") Long usuarioId);

    @Query("select v.sesionCaja.id from Venta v where v.id = :id")
    Optional<Long> findSesionCajaId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venta v where v.id = :id")
    Optional<Venta> findByIdForUpdate(@Param("id") Long id);
}
