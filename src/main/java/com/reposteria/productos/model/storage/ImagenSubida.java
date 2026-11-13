package com.reposteria.productos.model.storage;

/** Imagen ya validada, lista para guardarse. */
public record ImagenSubida(byte[] contenido, FormatoImagen formato) {
}
