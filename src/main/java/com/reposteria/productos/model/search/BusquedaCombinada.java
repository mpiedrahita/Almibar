package com.reposteria.productos.model.search;

import com.reposteria.productos.model.entity.Producto;
import com.reposteria.productos.model.repository.ProductoRepository;
import com.reposteria.productos.model.repository.ProductoSpecifications;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Búsqueda con cualquier combinación de filtros (o ninguno).
 * Va de última: resuelve todo lo que las estrategias más específicas no resuelven.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class BusquedaCombinada implements BusquedaProductoStrategy {

    private final ProductoRepository repositorio;

    public BusquedaCombinada(ProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean aplicaA(CriteriosBusqueda criterios) {
        return true;
    }

    @Override
    public Page<Producto> buscar(CriteriosBusqueda criterios, Pageable pagina) {
        return repositorio.findAll(ProductoSpecifications.segun(criterios), pagina);
    }
}
