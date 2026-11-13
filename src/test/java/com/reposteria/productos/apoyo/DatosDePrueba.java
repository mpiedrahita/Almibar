package com.reposteria.productos.apoyo;

import com.reposteria.productos.model.entity.Categoria;
import com.reposteria.productos.model.entity.Producto;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Crea objetos del dominio para las pruebas unitarias. */
public final class DatosDePrueba {

    public static final Categoria TORTAS = new Categoria(5L, "Tortas");

    private DatosDePrueba() {
    }

    public static Producto producto(long id, String nombre, boolean activo, boolean disponible) {
        Producto producto = Producto.registrar(nombre, "Descripción de prueba", TORTAS, new BigDecimal("45000"), disponible);
        if (!activo) {
            producto.desactivar();
            producto.cambiarDisponibilidad(disponible);
        }
        ReflectionTestUtils.setField(producto, "idProducto", id);
        ReflectionTestUtils.setField(producto, "fechaRegistro", LocalDateTime.of(2026, 11, 1, 9, 0));
        ReflectionTestUtils.setField(producto, "fechaActualizacion", LocalDateTime.of(2026, 11, 1, 9, 0));
        return producto;
    }
}
