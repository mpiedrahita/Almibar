package com.reposteria.productos.security.jwt;

import com.reposteria.productos.model.service.UsuarioActual;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Obtiene el usuario autenticado desde Spring Security, para la auditoría. */
@Component
public class UsuarioActualSeguridad implements UsuarioActual {

    @Override
    public String nombreUsuario() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated() || autenticacion.getName() == null) {
            return "sistema";
        }
        return autenticacion.getName();
    }
}
