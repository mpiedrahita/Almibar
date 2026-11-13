package com.reposteria.productos.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Acceso a la documentación de la API (Swagger UI y OpenAPI).
 *
 * Solo atiende las rutas de la documentación y va antes de la cadena principal ({@link SecurityConfig}),
 * que sigue protegiendo todo lo demás sin cambios. La documentación es pública; los endpoints que
 * documenta siguen pidiendo token y rol como siempre.
 *
 * Usa su propia política de contenido (CSP) porque Swagger UI necesita estilos en línea, que la
 * política de la vitrina no permite. Los scripts siguen limitados a archivos propios.
 */
@Configuration
public class SwaggerSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain cadenaDocumentacion(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(cabeceras -> cabeceras
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                                        + "script-src 'self'; font-src 'self' data:; object-src 'none'; frame-ancestors 'none'")))
                .authorizeHttpRequests(acceso -> acceso.anyRequest().permitAll());
        return http.build();
    }
}
