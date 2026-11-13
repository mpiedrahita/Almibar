package com.reposteria.productos.security;

import com.reposteria.productos.apoyo.TokensDePrueba;
import com.reposteria.productos.config.PropiedadesApp;
import com.reposteria.productos.security.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tokens JWT")
class JwtServiceTest {

    private static PropiedadesApp propiedades(String secreto) {
        return new PropiedadesApp(new PropiedadesApp.Jwt(secreto, 60),
                new PropiedadesApp.Imagenes("local", "uploads", 5_242_880, null));
    }

    private final JwtService jwt = new JwtService(propiedades(TokensDePrueba.SECRETO));

    @Test
    void aceptaUnTokenValidoConSuRol() {
        String token = TokensDePrueba.admin().substring("Bearer ".length());

        JwtService.ResultadoToken resultado = jwt.validar(token);

        assertThat(resultado.token()).hasValueSatisfying(t -> {
            assertThat(t.usuario()).isEqualTo("admin");
            assertThat(t.rol()).isEqualTo("ADMIN");
        });
    }

    @Test
    void rechazaUnTokenVencidoConUnMensajeClaro() {
        JwtService.ResultadoToken resultado = jwt.validar(TokensDePrueba.vencido().substring("Bearer ".length()));

        assertThat(resultado.token()).isEmpty();
        assertThat(resultado.motivo()).contains("venció");
    }

    @Test
    void rechazaUnTokenAlterado() {
        JwtService.ResultadoToken resultado = jwt.validar(TokensDePrueba.alterado().substring("Bearer ".length()));

        assertThat(resultado.token()).isEmpty();
        assertThat(resultado.motivo()).contains("no es válida");
    }

    @Test
    void rechazaTextoQueNoEsUnToken() {
        assertThat(jwt.validar("esto-no-es-un-token").token()).isEmpty();
    }

    @Test
    void exigeUnSecretoDeAlMenos32Bytes() {
        assertThatThrownBy(() -> new JwtService(propiedades("Y29ydG8=")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void informaLaDuracionDelToken() {
        assertThat(jwt.duracionSegundos()).isEqualTo(3600);
    }
}
