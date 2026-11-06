package com.boutique.backend.proveedor.repository;

import com.boutique.backend.proveedor.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
}
