package com.reposteria.productos.security.jwt;

import com.reposteria.productos.config.PropiedadesApp;
import com.reposteria.productos.model.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Genera y valida los tokens JWT. El secreto llega por variable de entorno (RNF-09)
 * y el token vence según la configuración (RNF-P02: 1 hora por defecto).
 */
@Service
public class JwtService {

    private static final String CLAIM_ROL = "rol";

    private final SecretKey clave;
    private final Duration duracion;

    public JwtService(PropiedadesApp propiedades) {
        byte[] bytes = Decoders.BASE64.decode(propiedades.jwt().secreto());
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes en Base64.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracion = Duration.ofMinutes(propiedades.jwt().expiracionMinutos());
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getUsuario())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    /** Devuelve los datos del token si es válido; si venció o fue alterado, informa el motivo. */
    public ResultadoToken validar(String token) {
        try {
            Claims datos = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return new ResultadoToken(Optional.of(new TokenValido(datos.getSubject(), datos.get(CLAIM_ROL, String.class))), null);
        } catch (ExpiredJwtException e) {
            return new ResultadoToken(Optional.empty(), "Tu sesión venció. Vuelve a iniciar sesión.");
        } catch (JwtException | IllegalArgumentException e) {
            return new ResultadoToken(Optional.empty(), "La sesión no es válida. Vuelve a iniciar sesión.");
        }
    }

    public long duracionSegundos() {
        return duracion.toSeconds();
    }

    /** Usuario y rol que trae un token válido. */
    public record TokenValido(String usuario, String rol) {
    }

    /** Resultado de validar: el token válido o el motivo por el que no lo es. */
    public record ResultadoToken(Optional<TokenValido> token, String motivo) {
    }
}
