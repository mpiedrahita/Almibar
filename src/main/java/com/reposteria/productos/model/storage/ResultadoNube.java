package com.reposteria.productos.model.storage;

/** Respuesta del servicio externo al subir un archivo. */
public record ResultadoNube(String urlSegura, String idPublico) {
}
