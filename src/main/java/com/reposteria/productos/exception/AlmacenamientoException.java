package com.reposteria.productos.exception;

/**
 * No se pudo guardar o borrar una imagen. Se responde con un error controlado
 * (HTTP 502), sin exponer rutas internas (RNF-19).
 */
public class AlmacenamientoException extends RuntimeException {

    public AlmacenamientoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
