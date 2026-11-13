package com.reposteria.productos.model.repository;

import com.reposteria.productos.model.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Patrón Repository: el servicio consulta y guarda productos sin conocer el SQL.
 * Spring Data JPA genera la implementación. Todas las consultas son paginadas (RNF-23)
 * y traen la categoría en la misma consulta (@EntityGraph) para no repetir consultas.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    @Override
    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Producto> findAll(Specification<Producto> especificacion, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Producto> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Producto> findByCategoria_IdCategoria(Long idCategoria, Pageable pageable);

    /** RF-11: solo los que se pueden vender. */
    @EntityGraph(attributePaths = "categoria")
    Page<Producto> findByActivoTrueAndDisponibilidadTrue(Pageable pageable);

    long countByActivoTrueAndDisponibilidadTrue();

    long countByActivoTrueAndDisponibilidadFalse();

    long countByActivoFalse();
}
