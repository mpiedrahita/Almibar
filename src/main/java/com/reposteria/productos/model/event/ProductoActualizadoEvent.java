package com.reposteria.productos.model.event;

import com.reposteria.productos.model.entity.TipoOperacion;

import java.time.LocalDateTime;

/**
 * Se modificó un producto: edición de datos, cambio de disponibilidad o cambio de imagen.
 * La operación concreta va en "operacion".
 */
public record ProductoActualizadoEvent(Long productoId, String nombreProducto, String usuario, LocalDateTime fecha,
                                       TipoOperacion operacion, String detalle) implements ProductoEvent {
}
