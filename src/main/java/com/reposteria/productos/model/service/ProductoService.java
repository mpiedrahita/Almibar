package com.reposteria.productos.model.service;

import com.reposteria.productos.dto.request.ProductoRequest;
import com.reposteria.productos.dto.response.PaginaResponse;
import com.reposteria.productos.dto.response.ProductoResponse;
import com.reposteria.productos.dto.response.ResumenVitrinaResponse;
import com.reposteria.productos.model.search.CriteriosBusqueda;
import com.reposteria.productos.model.storage.ArchivoRecibido;
import org.springframework.data.domain.Pageable;

/**
 * Operaciones del módulo de productos. El controlador depende de esta interfaz,
 * no de la implementación (DIP).
 */
public interface ProductoService {

    /** RF-05 y RF-06: listado paginado con filtros combinables. */
    PaginaResponse<ProductoResponse> buscar(CriteriosBusqueda criterios, Pageable pagina);

    /** RF-11: solo activos y disponibles. */
    PaginaResponse<ProductoResponse> listarDisponibles(Pageable pagina);

    ResumenVitrinaResponse resumen();

    /** RF-02. */
    ProductoResponse obtener(Long id);

    /** RF-01. */
    ProductoResponse registrar(ProductoRequest datos);

    /** RF-03. */
    ProductoResponse actualizar(Long id, ProductoRequest datos);

    /** RF-04: eliminación lógica. */
    ProductoResponse desactivar(Long id);

    /** RF-P02. */
    ProductoResponse reactivar(Long id);

    /** RF-P03, aplica la regla de negocio principal. */
    ProductoResponse cambiarDisponibilidad(Long id, boolean disponible);

    /** RF-10. */
    ProductoResponse actualizarImagen(Long id, ArchivoRecibido archivo);
}
