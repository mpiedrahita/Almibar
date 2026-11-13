package com.reposteria.productos.model.search;

import java.util.Locale;

/**
 * Filtros de búsqueda (RF-05). Cualquiera puede venir vacío.
 *
 * @param nombre      texto que debe contener el nombre (sin distinguir mayúsculas)
 * @param categoriaId categoría exacta
 * @param disponible  true = disponible, false = agotado
 * @param activo      true = en la carta, false = fuera de carta
 */
public record CriteriosBusqueda(String nombre, Long categoriaId, Boolean disponible, Boolean activo) {

    public static CriteriosBusqueda vacios() {
        return new CriteriosBusqueda(null, null, null, null);
    }

    public boolean tieneNombre() {
        return nombre != null && !nombre.isBlank();
    }

    public boolean tieneCategoria() {
        return categoriaId != null;
    }

    public boolean tieneEstado() {
        return disponible != null || activo != null;
    }

    public String nombreNormalizado() {
        return tieneNombre() ? nombre.trim().toLowerCase(Locale.ROOT) : "";
    }

    /** Solo se busca por nombre. */
    public boolean soloNombre() {
        return tieneNombre() && !tieneCategoria() && !tieneEstado();
    }

    /** Solo se filtra por categoría. */
    public boolean soloCategoria() {
        return tieneCategoria() && !tieneNombre() && !tieneEstado();
    }
}
