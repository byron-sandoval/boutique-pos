package com.boutique.backend.venta.repository;

import java.util.List;

import com.boutique.backend.venta.entity.DetalleDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleDevolucionRepository extends JpaRepository<DetalleDevolucion, Long> {

    List<DetalleDevolucion> findByDevolucionIdOrderByIdAsc(Long devolucionId);

    List<DetalleDevolucion> findByDevolucionVentaIdOrderByIdAsc(Long ventaId);
}
