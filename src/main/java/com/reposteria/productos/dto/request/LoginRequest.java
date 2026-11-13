package com.reposteria.productos.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Credenciales para iniciar sesión (RF-P04). */
public record LoginRequest(
        @NotBlank(message = "Escribe tu usuario.")
        String usuario,

        @NotBlank(message = "Escribe tu contraseña.")
        String contrasena
) {
}
