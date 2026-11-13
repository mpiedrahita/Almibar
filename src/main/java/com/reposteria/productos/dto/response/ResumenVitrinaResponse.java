package com.reposteria.productos.dto.response;

/** Cuántos productos hay en cada estado; alimenta las pestañas de la vitrina. */
public record ResumenVitrinaResponse(long total, long enVitrina, long agotados, long fueraDeCarta) {
}
