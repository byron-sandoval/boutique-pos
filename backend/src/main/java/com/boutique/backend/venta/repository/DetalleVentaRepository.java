package com.boutique.backend.venta.repository;

import java.util.List;

import com.boutique.backend.venta.entity.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    List<DetalleVenta> findByVentaIdOrderByIdAsc(Long ventaId);
}
