package com.reposteria.productos.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración propia de la aplicación (prefijo "app").
 * Los valores sensibles llegan por variables de entorno (RNF-09, RNF-20).
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record PropiedadesApp(@Valid @NotNull Jwt jwt, @Valid @NotNull Imagenes imagenes) {

    /** Secreto en Base64 (mínimo 32 bytes) y duración del token. */
    public record Jwt(@NotBlank String secreto, @Min(1) long expiracionMinutos) {
    }

    /**
     * proveedor: "local" (carpeta del servidor) o "nube" (Cloudinary).
     * tamanoMaximoBytes: límite por archivo (RNF-21: 5 MB).
     */
    public record Imagenes(@NotBlank String proveedor,
                           @NotBlank String directorio,
                           @Min(1) long tamanoMaximoBytes,
                           Nube nube) {
    }

    /** Credenciales de Cloudinary; solo son obligatorias si proveedor = "nube". */
    public record Nube(String nombreCuenta, String claveApi, String secretoApi) {
    }
}
