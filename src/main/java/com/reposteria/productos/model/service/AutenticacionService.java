package com.reposteria.productos.model.service;

import com.reposteria.productos.dto.request.LoginRequest;
import com.reposteria.productos.dto.response.LoginResponse;
import com.reposteria.productos.dto.response.UsuarioResponse;
import com.reposteria.productos.model.entity.Usuario;
import com.reposteria.productos.model.repository.UsuarioRepository;
import com.reposteria.productos.security.jwt.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicio de sesión (RF-P04): valida las credenciales y entrega un token JWT. */
@Service
public class AutenticacionService {

    private final AuthenticationManager autenticador;
    private final UsuarioRepository usuarios;
    private final JwtService jwt;

    public AutenticacionService(AuthenticationManager autenticador, UsuarioRepository usuarios, JwtService jwt) {
        this.autenticador = autenticador;
        this.usuarios = usuarios;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public LoginResponse iniciarSesion(LoginRequest credenciales) {
        // Lanza BadCredentialsException (401) si el usuario o la contraseña no coinciden.
        autenticador.authenticate(new UsernamePasswordAuthenticationToken(
                credenciales.usuario().trim(), credenciales.contrasena()));
        Usuario usuario = usuarios.findByUsuario(credenciales.usuario().trim())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos."));
        String token = jwt.generar(usuario);
        return new LoginResponse(token, "Bearer", jwt.duracionSegundos(), aRespuesta(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse datosDe(String nombreUsuario) {
        return usuarios.findByUsuario(nombreUsuario)
                .map(this::aRespuesta)
                .orElseThrow(() -> new BadCredentialsException("La sesión no es válida."));
    }

    private UsuarioResponse aRespuesta(Usuario usuario) {
        return new UsuarioResponse(usuario.getUsuario(), usuario.getNombre(), usuario.getRol().name());
    }
}
