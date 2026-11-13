package com.reposteria.productos.model.search;

import com.reposteria.productos.model.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Patrón Strategy: cada forma de buscar productos es una estrategia intercambiable.
 * Para agregar un criterio nuevo se crea otra implementación, sin tocar las demás (OCP).
 * La interfaz solo tiene lo necesario para buscar (ISP).
 */
public interface BusquedaProductoStrategy {

    /** Indica si esta estrategia sabe resolver estos criterios. */
    boolean aplicaA(CriteriosBusqueda criterios);

    Page<Producto> buscar(CriteriosBusqueda criterios, Pageable pagina);
}
