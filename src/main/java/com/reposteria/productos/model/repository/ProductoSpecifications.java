package com.reposteria.productos.model.repository;

import com.reposteria.productos.model.entity.Producto;
import com.reposteria.productos.model.search.CriteriosBusqueda;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye la consulta dinámica para la búsqueda combinada.
 * Los filtros se aplican en la base de datos, no en memoria (sección 4, rendimiento).
 */
public final class ProductoSpecifications {

    private ProductoSpecifications() {
    }

    public static Specification<Producto> segun(CriteriosBusqueda criterios) {
        return (raiz, consulta, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (criterios.tieneNombre()) {
                filtros.add(cb.like(cb.lower(raiz.get("nombre")), "%" + criterios.nombreNormalizado() + "%"));
            }
            if (criterios.tieneCategoria()) {
                filtros.add(cb.equal(raiz.get("categoria").get("idCategoria"), criterios.categoriaId()));
            }
            if (criterios.disponible() != null) {
                filtros.add(cb.equal(raiz.get("disponibilidad"), criterios.disponible()));
            }
            if (criterios.activo() != null) {
                filtros.add(cb.equal(raiz.get("activo"), criterios.activo()));
            }
            return cb.and(filtros.toArray(new Predicate[0]));
        };
    }
}
