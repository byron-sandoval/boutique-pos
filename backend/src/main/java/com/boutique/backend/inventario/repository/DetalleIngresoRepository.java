package com.boutique.backend.inventario.repository;

import java.util.List;

import com.boutique.backend.inventario.entity.DetalleIngreso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleIngresoRepository extends JpaRepository<DetalleIngreso, Long> {

    List<DetalleIngreso> findByIngresoIdOrderByIdAsc(Long ingresoId);
}
