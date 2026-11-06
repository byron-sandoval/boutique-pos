package com.boutique.backend.inventario.repository;

import java.util.Optional;

import com.boutique.backend.inventario.entity.Ingreso;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngresoRepository extends JpaRepository<Ingreso, Long> {

    Page<Ingreso> findBySucursalId(Long sucursalId, Pageable pageable);

    boolean existsBySucursalIdAndProveedorIdAndNoDocumentoAndActivoTrue(
        Long sucursalId, Long proveedorId, String noDocumento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Ingreso i where i.id = :id")
    Optional<Ingreso> findByIdForUpdate(@Param("id") Long id);
}
