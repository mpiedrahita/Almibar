package com.reposteria.productos.controller;

import com.reposteria.productos.dto.request.DisponibilidadRequest;
import com.reposteria.productos.dto.request.ProductoRequest;
import com.reposteria.productos.dto.response.PaginaResponse;
import com.reposteria.productos.dto.response.ProductoResponse;
import com.reposteria.productos.dto.response.ResumenVitrinaResponse;
import com.reposteria.productos.model.search.CriteriosBusqueda;
import com.reposteria.productos.model.service.ProductoService;
import com.reposteria.productos.model.storage.ArchivoRecibido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

/**
 * Controlador de productos (la C de MVC): recibe la petición, revisa su formato,
 * llama al servicio y responde. No contiene reglas de negocio ni consultas a MySQL.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private static final Sort ORDEN = Sort.by("nombre").ascending();

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    /** RF-05 y RF-06: listado paginado con búsqueda por nombre, categoría, disponibilidad y estado. */
    @GetMapping
    public PaginaResponse<ProductoResponse> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño mínimo de página es 1.")
            @Max(value = 100, message = "El tamaño máximo de página es 100.") int size) {
        return servicio.buscar(new CriteriosBusqueda(nombre, categoriaId, disponible, activo), pagina(page, size));
    }

    /** RF-11: solo productos activos y disponibles (lo que usaría el módulo de pedidos). */
    @GetMapping("/disponibles")
    public PaginaResponse<ProductoResponse> disponibles(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño mínimo de página es 1.")
            @Max(value = 100, message = "El tamaño máximo de página es 100.") int size) {
        return servicio.listarDisponibles(pagina(page, size));
    }

    /** Conteo por estado para las pestañas de la vitrina. */
    @GetMapping("/resumen")
    public ResumenVitrinaResponse resumen() {
        return servicio.resumen();
    }

    /** RF-02 y RF-08. */
    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /** RF-01: responde 201 con la dirección del producto creado. */
    @PostMapping
    public ResponseEntity<ProductoResponse> registrar(@Valid @RequestBody ProductoRequest datos) {
        ProductoResponse creado = servicio.registrar(datos);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    /** RF-03. */
    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest datos) {
        return servicio.actualizar(id, datos);
    }

    /** RF-04: eliminación lógica, el producto se conserva. */
    @DeleteMapping("/{id}")
    public ProductoResponse desactivar(@PathVariable Long id) {
        return servicio.desactivar(id);
    }

    /** RF-P02. */
    @PatchMapping("/{id}/reactivar")
    public ProductoResponse reactivar(@PathVariable Long id) {
        return servicio.reactivar(id);
    }

    /** RF-P03: aquí se aplica la regla "un producto inactivo no puede estar disponible" (409). */
    @PatchMapping("/{id}/disponibilidad")
    public ProductoResponse cambiarDisponibilidad(@PathVariable Long id,
                                                  @Valid @RequestBody DisponibilidadRequest datos) {
        return servicio.cambiarDisponibilidad(id, datos.disponible());
    }

    /** RF-10: sube o reemplaza la imagen (JPG, PNG o WEBP de máximo 5 MB). */
    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductoResponse subirImagen(@PathVariable Long id, @RequestPart("archivo") MultipartFile archivo)
            throws IOException {
        return servicio.actualizarImagen(id, new ArchivoRecibido(archivo.getBytes(), archivo.getOriginalFilename()));
    }

    private static Pageable pagina(int page, int size) {
        return PageRequest.of(page, size, ORDEN);
    }
}
