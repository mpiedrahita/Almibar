package com.reposteria.productos.dto.response;

import java.util.List;

/** Una página de resultados (RNF-23). */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
}
