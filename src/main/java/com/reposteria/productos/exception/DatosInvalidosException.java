package com.reposteria.productos.exception;

/**
 * Los datos tienen el formato correcto pero no son válidos para el negocio
 * (por ejemplo, una categoría que no existe o una imagen de un formato no permitido).
 * Se responde con HTTP 422 e indica el campo afectado.
 */
public class DatosInvalidosException extends RuntimeException {

    private final String campo;

    public DatosInvalidosException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
