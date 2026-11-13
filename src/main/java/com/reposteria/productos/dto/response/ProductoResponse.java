package com.reposteria.productos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Producto tal como lo ve la vista. "vendible" responde RF-08: está activo y disponible a la vez.
 */
public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        CategoriaResponse categoria,
        BigDecimal precio,
        boolean disponibilidad,
        boolean activo,
        boolean vendible,
        String imagenUrl,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacion
) {
}
