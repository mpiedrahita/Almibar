package com.reposteria.productos.dto.response;

/** Resultado de iniciar sesión: el token y los datos que la vista muestra. */
public record LoginResponse(String token, String tipo, long expiraEnSegundos, UsuarioResponse usuario) {
}
