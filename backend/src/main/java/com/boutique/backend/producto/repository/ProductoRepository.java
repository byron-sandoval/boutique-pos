package com.boutique.backend.producto.repository;

import java.util.Optional;

import com.boutique.backend.producto.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    boolean existsByImagenUrlAndIdNot(String imagenUrl, Long id);
    boolean existsByCategoriaIdAndActivoTrue(Long categoriaId);
    boolean existsByUnidadMedidaIdAndActivoTrue(Long unidadMedidaId);

    Page<Producto> findByCategoriaId(Long categoriaId, Pageable pageable);

    @Query("""
        select p from Producto p
        where p.categoria.id = coalesce(:categoriaId, p.categoria.id)
          and (locate(lower(:busqueda), lower(p.nombre)) > 0
               or locate(lower(:busqueda), lower(p.codigo)) > 0)
        """)
    Page<Producto> buscar(@Param("busqueda") String busqueda, @Param("categoriaId") Long categoriaId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
