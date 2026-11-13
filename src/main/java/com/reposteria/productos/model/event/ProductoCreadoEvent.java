package com.reposteria.productos.model.event;

import com.reposteria.productos.model.entity.TipoOperacion;

import java.time.LocalDateTime;

/** Se registró un producto nuevo. */
public record ProductoCreadoEvent(Long productoId, String nombreProducto, String usuario, LocalDateTime fecha)
        implements ProductoEvent {

    @Override
    public TipoOperacion operacion() {
        return TipoOperacion.CREACION;
    }

    @Override
    public String detalle() {
        return "Producto registrado";
    }
}
