package com.reposteria.productos.exception;

/** La imagen supera el tamaño máximo permitido (RNF-21). Se responde con HTTP 413. */
public class ArchivoDemasiadoGrandeException extends RuntimeException {

    public ArchivoDemasiadoGrandeException(String mensaje) {
        super(mensaje);
    }
}
