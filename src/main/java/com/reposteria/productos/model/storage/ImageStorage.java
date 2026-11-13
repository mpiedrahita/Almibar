package com.reposteria.productos.model.storage;

/**
 * Patrón Adapter, interfaz objetivo: la única forma en que el sistema guarda imágenes.
 * ProductoService depende de esta interfaz y no de un proveedor concreto (DIP).
 * Toda implementación debe cumplir el mismo contrato (LSP):
 * guardar devuelve la URL pública de la imagen y eliminar recibe esa misma URL.
 */
public interface ImageStorage {

    String guardar(ImagenSubida imagen);

    void eliminar(String url);
}
