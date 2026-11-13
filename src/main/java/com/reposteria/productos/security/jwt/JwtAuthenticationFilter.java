package com.reposteria.productos.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Middleware de seguridad: revisa el token de cada petición antes de que llegue al controlador.
 * Si es válido, deja autenticado al usuario con su rol. Si no, la petición sigue sin
 * autenticación y Spring Security responde 401 en las rutas protegidas.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Atributo con el motivo por el que el token no sirvió, para el mensaje del 401. */
    public static final String MOTIVO_TOKEN = "almibar.motivoToken";

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwt;

    public JwtAuthenticationFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String cabecera = peticion.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            JwtService.ResultadoToken resultado = jwt.validar(cabecera.substring(PREFIJO.length()).trim());
            resultado.token().ifPresentOrElse(token -> {
                var autenticacion = new UsernamePasswordAuthenticationToken(token.usuario(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + token.rol())));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }, () -> peticion.setAttribute(MOTIVO_TOKEN, resultado.motivo()));
        }
        cadena.doFilter(peticion, respuesta);
    }
}
