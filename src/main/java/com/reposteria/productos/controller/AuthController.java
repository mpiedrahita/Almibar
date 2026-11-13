package com.reposteria.productos.controller;

import com.reposteria.productos.dto.request.LoginRequest;
import com.reposteria.productos.dto.response.LoginResponse;
import com.reposteria.productos.dto.response.UsuarioResponse;
import com.reposteria.productos.model.service.AutenticacionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inicio de sesión (RF-P04) y datos del usuario conectado. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacionService servicio;

    public AuthController(AutenticacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    public LoginResponse iniciarSesion(@Valid @RequestBody LoginRequest credenciales) {
        return servicio.iniciarSesion(credenciales);
    }

    @GetMapping("/yo")
    public UsuarioResponse yo(Authentication autenticacion) {
        return servicio.datosDe(autenticacion.getName());
    }
}
