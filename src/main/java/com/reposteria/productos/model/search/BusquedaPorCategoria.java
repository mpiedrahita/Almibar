package com.reposteria.productos.model.search;

import com.reposteria.productos.exception.RecursoNoEncontradoException;
import com.reposteria.productos.model.entity.Producto;
import com.reposteria.productos.model.repository.CategoriaRepository;
import com.reposteria.productos.model.repository.ProductoRepository;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** Búsqueda solo por categoría: primero comprueba que la categoría exista. */
@Component
@Order(2)
public class BusquedaPorCategoria implements BusquedaProductoStrategy {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;

    public BusquedaPorCategoria(ProductoRepository productos, CategoriaRepository categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    @Override
    public boolean aplicaA(CriteriosBusqueda criterios) {
        return criterios.soloCategoria();
    }

    @Override
    public Page<Producto> buscar(CriteriosBusqueda criterios, Pageable pagina) {
        if (!categorias.existsById(criterios.categoriaId())) {
            throw new RecursoNoEncontradoException("La categoría " + criterios.categoriaId() + " no existe.");
        }
        return productos.findByCategoria_IdCategoria(criterios.categoriaId(), pagina);
    }
}
