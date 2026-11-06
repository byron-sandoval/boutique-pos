package com.boutique.backend.cliente.repository;

import com.boutique.backend.cliente.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCedula(String cedula);

    boolean existsByCedulaAndIdNot(String cedula, Long id);

    boolean existsByPasaporte(String pasaporte);

    boolean existsByPasaporteAndIdNot(String pasaporte, Long id);
}
