package com.boutique.backend.producto.repository;

import java.util.Optional;

import com.boutique.backend.producto.entity.Categoria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Categoria r where r.id = :id")
    Optional<Categoria> findByIdForUpdate(@Param("id") Long id);
}
