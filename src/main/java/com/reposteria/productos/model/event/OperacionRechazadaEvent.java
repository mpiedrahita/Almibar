package com.reposteria.productos.model.event;

import com.reposteria.productos.model.entity.TipoOperacion;

import java.time.LocalDateTime;

/**
 * Una operación administrativa fue rechazada por una regla de negocio.
 * Va aparte de ProductoEvent porque se registra aunque la transacción se revierta.
 */
public record OperacionRechazadaEvent(Long productoId, String nombreProducto, String usuario, LocalDateTime fecha,
                                      TipoOperacion operacion, String motivo) {
}
