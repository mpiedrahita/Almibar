package com.reposteria.productos.model.service;

import com.reposteria.productos.dto.request.ProductoRequest;
import com.reposteria.productos.dto.response.PaginaResponse;
import com.reposteria.productos.dto.response.ProductoResponse;
import com.reposteria.productos.dto.response.ResumenVitrinaResponse;
import com.reposteria.productos.exception.DatosInvalidosException;
import com.reposteria.productos.exception.RecursoNoEncontradoException;
import com.reposteria.productos.exception.ReglaNegocioException;
import com.reposteria.productos.mapper.ProductoMapper;
import com.reposteria.productos.model.entity.Categoria;
import com.reposteria.productos.model.entity.Producto;
import com.reposteria.productos.model.entity.TipoOperacion;
import com.reposteria.productos.model.event.OperacionRechazadaEvent;
import com.reposteria.productos.model.event.ProductoActualizadoEvent;
import com.reposteria.productos.model.event.ProductoCreadoEvent;
import com.reposteria.productos.model.event.ProductoDesactivadoEvent;
import com.reposteria.productos.model.event.ProductoReactivadoEvent;
import com.reposteria.productos.model.repository.CategoriaRepository;
import com.reposteria.productos.model.repository.ProductoRepository;
import com.reposteria.productos.model.rules.ReglasProducto;
import com.reposteria.productos.model.search.BusquedaProductoStrategy;
import com.reposteria.productos.model.search.CriteriosBusqueda;
import com.reposteria.productos.model.storage.ArchivoRecibido;
import com.reposteria.productos.model.storage.ImageStorage;
import com.reposteria.productos.model.storage.ImagenSubida;
import com.reposteria.productos.model.storage.ImagenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de negocio del módulo de productos (parte del Modelo en MVC).
 *
 * - Usa los repositorios para los datos (Repository).
 * - Elige la estrategia de búsqueda según los filtros (Strategy).
 * - Publica eventos cuando cambia un producto (Observer: sujeto).
 * - Guarda imágenes a través de ImageStorage sin saber el proveedor (Adapter).
 *
 * Las escrituras son transaccionales: se guardan completas o se revierten (RNF-11).
 */
@Service
public class ProductoServiceImpl implements ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class);

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final ReglasProducto reglas;
    private final List<BusquedaProductoStrategy> estrategias;
    private final ImageStorage almacenImagenes;
    private final ImagenValidator validadorImagenes;
    private final ApplicationEventPublisher eventos;
    private final UsuarioActual usuarioActual;
    private final ProductoMapper mapper;

    public ProductoServiceImpl(ProductoRepository productos, CategoriaRepository categorias, ReglasProducto reglas,
                               List<BusquedaProductoStrategy> estrategias, ImageStorage almacenImagenes,
                               ImagenValidator validadorImagenes, ApplicationEventPublisher eventos,
                               UsuarioActual usuarioActual, ProductoMapper mapper) {
        this.productos = productos;
        this.categorias = categorias;
        this.reglas = reglas;
        this.estrategias = estrategias;
        this.almacenImagenes = almacenImagenes;
        this.validadorImagenes = validadorImagenes;
        this.eventos = eventos;
        this.usuarioActual = usuarioActual;
        this.mapper = mapper;
    }

    // ---------- Consultas ----------

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ProductoResponse> buscar(CriteriosBusqueda criterios, Pageable pagina) {
        CriteriosBusqueda filtros = criterios == null ? CriteriosBusqueda.vacios() : criterios;
        BusquedaProductoStrategy estrategia = estrategias.stream()
                .filter(e -> e.aplicaA(filtros))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay una estrategia de búsqueda para estos filtros"));
        return mapper.aPagina(estrategia.buscar(filtros, pagina));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ProductoResponse> listarDisponibles(Pageable pagina) {
        return mapper.aPagina(productos.findByActivoTrueAndDisponibilidadTrue(pagina));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenVitrinaResponse resumen() {
        return new ResumenVitrinaResponse(
                productos.count(),
                productos.countByActivoTrueAndDisponibilidadTrue(),
                productos.countByActivoTrueAndDisponibilidadFalse(),
                productos.countByActivoFalse());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return mapper.aRespuesta(buscarProducto(id));
    }

    // ---------- Escrituras ----------

    @Override
    @Transactional
    public ProductoResponse registrar(ProductoRequest datos) {
        Categoria categoria = buscarCategoria(datos.categoriaId());
        reglas.validarPrecio(datos.precio());
        Producto producto = Producto.registrar(datos.nombre(), datos.descripcion(), categoria,
                datos.precio(), Boolean.TRUE.equals(datos.disponible()));
        Producto guardado = productos.saveAndFlush(producto);
        eventos.publishEvent(new ProductoCreadoEvent(guardado.getIdProducto(), guardado.getNombre(),
                usuarioActual.nombreUsuario(), LocalDateTime.now()));
        return mapper.aRespuesta(guardado);
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest datos) {
        Producto producto = buscarProducto(id);
        Categoria categoria = buscarCategoria(datos.categoriaId());
        reglas.validarPrecio(datos.precio());
        boolean disponible = Boolean.TRUE.equals(datos.disponible());
        aplicarRegla(TipoOperacion.EDICION, producto, () -> reglas.validarDisponibilidad(producto, disponible));
        producto.actualizar(datos.nombre(), datos.descripcion(), categoria, datos.precio(), disponible);
        Producto guardado = productos.saveAndFlush(producto);
        publicarActualizacion(guardado, TipoOperacion.EDICION, "Datos del producto editados");
        return mapper.aRespuesta(guardado);
    }

    @Override
    @Transactional
    public ProductoResponse desactivar(Long id) {
        Producto producto = buscarProducto(id);
        aplicarRegla(TipoOperacion.DESACTIVACION, producto, () -> reglas.validarDesactivacion(producto));
        producto.desactivar();
        Producto guardado = productos.saveAndFlush(producto);
        eventos.publishEvent(new ProductoDesactivadoEvent(guardado.getIdProducto(), guardado.getNombre(),
                usuarioActual.nombreUsuario(), LocalDateTime.now()));
        return mapper.aRespuesta(guardado);
    }

    @Override
    @Transactional
    public ProductoResponse reactivar(Long id) {
        Producto producto = buscarProducto(id);
        aplicarRegla(TipoOperacion.REACTIVACION, producto, () -> reglas.validarReactivacion(producto));
        producto.reactivar();
        Producto guardado = productos.saveAndFlush(producto);
        eventos.publishEvent(new ProductoReactivadoEvent(guardado.getIdProducto(), guardado.getNombre(),
                usuarioActual.nombreUsuario(), LocalDateTime.now()));
        return mapper.aRespuesta(guardado);
    }

    @Override
    @Transactional
    public ProductoResponse cambiarDisponibilidad(Long id, boolean disponible) {
        Producto producto = buscarProducto(id);
        aplicarRegla(TipoOperacion.CAMBIO_DISPONIBILIDAD, producto,
                () -> reglas.validarDisponibilidad(producto, disponible));
        producto.cambiarDisponibilidad(disponible);
        Producto guardado = productos.saveAndFlush(producto);
        publicarActualizacion(guardado, TipoOperacion.CAMBIO_DISPONIBILIDAD,
                disponible ? "Marcado como disponible" : "Marcado como agotado");
        return mapper.aRespuesta(guardado);
    }

    @Override
    @Transactional
    public ProductoResponse actualizarImagen(Long id, ArchivoRecibido archivo) {
        Producto producto = buscarProducto(id);
        ImagenSubida imagen = validadorImagenes.validar(archivo);
        String imagenAnterior = producto.getImagenUrl();
        String imagenNueva = almacenImagenes.guardar(imagen);
        try {
            producto.cambiarImagen(imagenNueva);
            productos.saveAndFlush(producto);
        } catch (RuntimeException e) {
            // Si no se pudo guardar el producto, no se deja la imagen nueva huérfana.
            borrarImagen(imagenNueva);
            throw e;
        }
        // La imagen anterior solo se borra si el cambio queda confirmado en la base de datos.
        alTerminarTransaccion(() -> borrarImagen(imagenAnterior), () -> borrarImagen(imagenNueva));
        publicarActualizacion(producto, TipoOperacion.CAMBIO_IMAGEN, "Imagen actualizada");
        return mapper.aRespuesta(producto);
    }

    // ---------- Apoyo ----------

    private Producto buscarProducto(Long id) {
        return productos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un producto con el id " + id + "."));
    }

    private Categoria buscarCategoria(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new DatosInvalidosException("categoriaId", "La categoría elegida no existe."));
    }

    /** Ejecuta una regla; si la rechaza, deja constancia en la auditoría y propaga el error. */
    private void aplicarRegla(TipoOperacion operacion, Producto producto, Runnable regla) {
        try {
            regla.run();
        } catch (ReglaNegocioException e) {
            eventos.publishEvent(new OperacionRechazadaEvent(producto.getIdProducto(), producto.getNombre(),
                    usuarioActual.nombreUsuario(), LocalDateTime.now(), operacion, e.getMessage()));
            throw e;
        }
    }

    private void publicarActualizacion(Producto producto, TipoOperacion operacion, String detalle) {
        eventos.publishEvent(new ProductoActualizadoEvent(producto.getIdProducto(), producto.getNombre(),
                usuarioActual.nombreUsuario(), LocalDateTime.now(), operacion, detalle));
    }

    private void borrarImagen(String url) {
        if (url == null) {
            return;
        }
        try {
            almacenImagenes.eliminar(url);
        } catch (RuntimeException e) {
            log.warn("No se pudo borrar una imagen del producto: {}", e.getMessage());
        }
    }

    /** Ejecuta una acción cuando la transacción termina: una si se confirmó y otra si se revirtió. */
    private void alTerminarTransaccion(Runnable siSeConfirma, Runnable siSeRevierte) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            siSeConfirma.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado == STATUS_COMMITTED) {
                    siSeConfirma.run();
                } else {
                    siSeRevierte.run();
                }
            }
        });
    }
}
