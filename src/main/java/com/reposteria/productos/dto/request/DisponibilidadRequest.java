package com.reposteria.productos.dto.request;

import jakarta.validation.constraints.NotNull;

/** Cambio rápido de disponibilidad (RF-P03). */
public record DisponibilidadRequest(
        @NotNull(message = "Indica si el producto queda disponible.")
        Boolean disponible
) {
}
