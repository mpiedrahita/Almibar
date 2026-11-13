package com.reposteria.productos.model.service;

/**
 * Quién está haciendo la operación. Es una interfaz para que el servicio no dependa de
 * Spring Security (DIP); la implementación está en el paquete security.
 */
public interface UsuarioActual {

    String nombreUsuario();
}
