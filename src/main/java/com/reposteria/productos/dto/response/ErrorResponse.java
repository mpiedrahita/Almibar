package com.reposteria.productos.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Formato único para todas las respuestas de error (RNF-P01, RNF-19).
 * Nunca incluye trazas, consultas SQL, rutas internas ni tokens.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int estado,
        String error,
        String mensaje,
        String codigo,
        List<CampoError> campos,
        String ruta,
        Instant fecha
) {

    /** Error de un campo concreto del formulario. */
    public record CampoError(String campo, String mensaje) {
    }

    public static ErrorResponse de(int estado, String error, String mensaje, String ruta) {
        return new ErrorResponse(estado, error, mensaje, null, null, ruta, Instant.now());
    }
}
