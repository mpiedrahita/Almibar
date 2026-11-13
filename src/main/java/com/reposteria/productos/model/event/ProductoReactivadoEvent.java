package com.reposteria.productos.model.event;

import com.reposteria.productos.model.entity.TipoOperacion;

import java.time.LocalDateTime;

/** Se reactivó un producto (vuelve a la carta, todavía sin estar disponible). */
public record ProductoReactivadoEvent(Long productoId, String nombreProducto, String usuario, LocalDateTime fecha)
        implements ProductoEvent {

    @Override
    public TipoOperacion operacion() {
        return TipoOperacion.REACTIVACION;
    }

    @Override
    public String detalle() {
        return "Producto reactivado (vuelve a la carta)";
    }
}
