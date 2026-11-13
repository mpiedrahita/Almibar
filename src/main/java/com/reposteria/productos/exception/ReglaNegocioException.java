package com.reposteria.productos.exception;

/**
 * La operación choca con una regla de negocio (por ejemplo, marcar disponible un
 * producto inactivo). Se responde con HTTP 409. El código permite que la vista
 * reconozca el caso sin depender del texto.
 */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
