package com.reposteria.productos.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos para registrar o editar un producto (RF-01, RF-03).
 * Estas anotaciones revisan el formato (RF-07); las reglas de negocio las revisa el servicio.
 */
public record ProductoRequest(

        @NotBlank(message = "Escribe el nombre del producto.")
        @Size(max = 100, message = "El nombre puede tener máximo 100 caracteres.")
        String nombre,

        @Size(max = 255, message = "La descripción puede tener máximo 255 caracteres.")
        String descripcion,

        @NotNull(message = "Elige una categoría.")
        Long categoriaId,

        @NotNull(message = "Escribe el precio.")
        @Positive(message = "El precio debe ser mayor que cero.")
        @Digits(integer = 8, fraction = 2, message = "El precio admite máximo 8 cifras y dos decimales.")
        BigDecimal precio,

        @NotNull(message = "Indica si el producto está disponible.")
        Boolean disponible
) {
}
