package com.reposteria.productos.exception;

/** Lo que se busca no existe. Se responde con HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
