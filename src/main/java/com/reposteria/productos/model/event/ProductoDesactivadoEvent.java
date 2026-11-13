package com.reposteria.productos.model.event;

import com.reposteria.productos.model.entity.TipoOperacion;

import java.time.LocalDateTime;

/** Se desactivó un producto (sale de la carta). */
public record ProductoDesactivadoEvent(Long productoId, String nombreProducto, String usuario, LocalDateTime fecha)
        implements ProductoEvent {

    @Override
    public TipoOperacion operacion() {
        return TipoOperacion.DESACTIVACION;
    }

    @Override
    public String detalle() {
        return "Producto desactivado (fuera de carta)";
    }
}
