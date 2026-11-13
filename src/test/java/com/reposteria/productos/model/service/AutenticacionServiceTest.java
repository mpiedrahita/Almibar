package com.reposteria.productos.model.service;

import com.reposteria.productos.dto.request.LoginRequest;
import com.reposteria.productos.dto.response.LoginResponse;
import com.reposteria.productos.model.entity.Rol;
import com.reposteria.productos.model.entity.Usuario;
import com.reposteria.productos.model.repository.UsuarioRepository;
import com.reposteria.productos.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutenticacionServiceTest {

    private final AuthenticationManager autenticador = mock(AuthenticationManager.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AutenticacionService servicio = new AutenticacionService(autenticador, usuarios, jwt);

    private static Usuario usuario(String nombreUsuario, Rol rol) {
        Usuario usuario = BeanUtils.instantiateClass(Usuario.class);
        ReflectionTestUtils.setField(usuario, "usuario", nombreUsuario);
        ReflectionTestUtils.setField(usuario, "nombre", "Ana");
        ReflectionTestUtils.setField(usuario, "rol", rol);
        ReflectionTestUtils.setField(usuario, "activo", true);
        return usuario;
    }

    @Test
    void entregaElTokenYLosDatosDelUsuario() {
        when(usuarios.findByUsuario("admin")).thenReturn(Optional.of(usuario("admin", Rol.ADMIN)));
        when(jwt.generar(any())).thenReturn("token-de-prueba");
        when(jwt.duracionSegundos()).thenReturn(3600L);

        LoginResponse respuesta = servicio.iniciarSesion(new LoginRequest(" admin ", "Almibar.2026"));

        assertThat(respuesta.token()).isEqualTo("token-de-prueba");
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.usuario().rol()).isEqualTo("ADMIN");
    }

    @Test
    void credencialesIncorrectasLanzanError() {
        when(autenticador.authenticate(any())).thenThrow(new BadCredentialsException("mal"));

        assertThatThrownBy(() -> servicio.iniciarSesion(new LoginRequest("admin", "mala")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void devuelveLosDatosDelUsuarioConectado() {
        when(usuarios.findByUsuario("consulta")).thenReturn(Optional.of(usuario("consulta", Rol.CONSULTA)));

        assertThat(servicio.datosDe("consulta").rol()).isEqualTo("CONSULTA");
        assertThatThrownBy(() -> servicio.datosDe("nadie")).isInstanceOf(BadCredentialsException.class);
    }
}
