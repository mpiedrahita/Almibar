package com.reposteria.productos.model.repository;

import com.reposteria.productos.model.entity.AuditoriaProducto;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos del registro de actividades (RNF-18). */
public interface AuditoriaProductoRepository extends JpaRepository<AuditoriaProducto, Long> {
}
