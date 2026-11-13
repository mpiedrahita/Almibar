package com.reposteria.productos.model.repository;

import com.reposteria.productos.model.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a datos de categorías. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByNombreAsc();
}
