package com.reposteria.productos.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reposteria.productos.dto.response.ErrorResponse;
import com.reposteria.productos.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respuestas de seguridad con el mismo formato de error que el resto de la API:
 * 401 cuando falta la autenticación (RNF-06) y 403 cuando no hay permisos (RNF-07).
 */
@Component
public class RespuestasSeguridad {

    private final ObjectMapper json;

    public RespuestasSeguridad(ObjectMapper json) {
        this.json = json;
    }

    public AuthenticationEntryPoint noAutenticado() {
        return (peticion, respuesta, excepcion) -> {
            Object motivo = peticion.getAttribute(JwtAuthenticationFilter.MOTIVO_TOKEN);
            String mensaje = motivo != null ? motivo.toString() : "Inicia sesión para hacer esta operación.";
            escribir(peticion, respuesta, HttpStatus.UNAUTHORIZED, mensaje);
        };
    }

    public AccessDeniedHandler sinPermisos() {
        return (peticion, respuesta, excepcion) -> escribir(peticion, respuesta, HttpStatus.FORBIDDEN,
                "Tu usuario no tiene permiso para modificar productos.");
    }

    private void escribir(HttpServletRequest peticion, HttpServletResponse respuesta, HttpStatus estado, String mensaje)
            throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(respuesta.getOutputStream(),
                ErrorResponse.de(estado.value(), estado.getReasonPhrase(), mensaje, peticion.getRequestURI()));
    }
}
