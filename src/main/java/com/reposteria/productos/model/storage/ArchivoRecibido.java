package com.reposteria.productos.model.storage;

/**
 * Archivo tal como llega desde la vista, sin validar. Así el servicio no depende
 * de las clases de HTTP (MultipartFile).
 */
public record ArchivoRecibido(byte[] contenido, String nombreOriginal) {
}
