package com.boutique.backend.empresa.repository;

import java.util.Optional;

import com.boutique.backend.empresa.entity.Empresa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Empresa> findOneById(Long id);

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, Long id);
    boolean existsByLogoUrlAndIdNot(String logoUrl, Long id);
}
