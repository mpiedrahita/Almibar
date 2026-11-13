package com.reposteria.productos.model.event;

import com.reposteria.productos.model.entity.TipoOperacion;

import java.time.LocalDateTime;

/**
 * Patrón Observer: evento que publica el servicio (sujeto) cuando una operación sobre
 * un producto termina bien. Los observadores lo reciben sin que el servicio los conozca.
 */
public interface ProductoEvent {

    Long productoId();

    String nombreProducto();

    String usuario();

    LocalDateTime fecha();

    TipoOperacion operacion();

    /** Descripción corta de lo que cambió. */
    String detalle();
}
