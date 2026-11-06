package com.boutique.backend.empresa.repository;

import java.util.Optional;

import com.boutique.backend.empresa.entity.Sucursal;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    Page<Sucursal> findByEmpresaId(Long empresaId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sucursal s where s.id = :id")
    Optional<Sucursal> findByIdForUpdate(@Param("id") Long id);
}
