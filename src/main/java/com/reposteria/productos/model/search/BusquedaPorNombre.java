package com.reposteria.productos.model.search;

import com.reposteria.productos.model.entity.Producto;
import com.reposteria.productos.model.repository.ProductoRepository;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** Búsqueda solo por nombre: usa una consulta directa sobre el índice de nombre. */
@Component
@Order(1)
public class BusquedaPorNombre implements BusquedaProductoStrategy {

    private final ProductoRepository repositorio;

    public BusquedaPorNombre(ProductoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean aplicaA(CriteriosBusqueda criterios) {
        return criterios.soloNombre();
    }

    @Override
    public Page<Producto> buscar(CriteriosBusqueda criterios, Pageable pagina) {
        return repositorio.findByNombreContainingIgnoreCase(criterios.nombre().trim(), pagina);
    }
}
