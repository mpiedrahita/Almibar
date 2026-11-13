package com.reposteria.productos.dto.response;

/** Usuario que inició sesión, sin datos sensibles. */
public record UsuarioResponse(String usuario, String nombre, String rol) {
}
