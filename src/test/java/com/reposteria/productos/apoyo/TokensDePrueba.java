package com.reposteria.productos.apoyo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/** Tokens firmados con el mismo secreto del perfil de pruebas (application-test.properties). */
public final class TokensDePrueba {

    public static final String SECRETO = "Y2xhdmUtZGUtcHJ1ZWJhcy1hbG1pYmFyLXNvbG8tcGFyYS10ZXN0cy0wMQ==";
    private static final SecretKey CLAVE = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRETO));

    private TokensDePrueba() {
    }

    public static String admin() {
        return "Bearer " + token("admin", "ADMIN", Instant.now().plusSeconds(3600));
    }

    public static String consulta() {
        return "Bearer " + token("consulta", "CONSULTA", Instant.now().plusSeconds(3600));
    }

    public static String vencido() {
        return "Bearer " + token("admin", "ADMIN", Instant.now().minusSeconds(60));
    }

    public static String alterado() {
        String bueno = token("admin", "ADMIN", Instant.now().plusSeconds(3600));
        return "Bearer " + bueno.substring(0, bueno.length() - 4) + "abcd";
    }

    private static String token(String usuario, String rol, Instant vence) {
        return Jwts.builder()
                .subject(usuario)
                .claim("rol", rol)
                .issuedAt(Date.from(vence.minusSeconds(7200)))
                .expiration(Date.from(vence))
                .signWith(CLAVE)
                .compact();
    }
}
