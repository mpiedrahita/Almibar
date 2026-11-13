package com.reposteria.productos.model.service;

import com.reposteria.productos.dto.request.ProductoRequest;
import com.reposteria.productos.dto.response.PaginaResponse;
import com.reposteria.productos.dto.response.ProductoResponse;
import com.reposteria.productos.dto.response.ResumenVitrinaResponse;
import com.reposteria.productos.exception.DatosInvalidosException;
import com.reposteria.productos.exception.RecursoNoEncontradoException;
import com.reposteria.productos.exception.ReglaNegocioException;
import com.reposteria.productos.mapper.ProductoMapper;
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
import com.reposteria.productos.model.storage.FormatoImagen;
import com.reposteria.productos.model.storage.ImageStorage;
import com.reposteria.productos.model.storage.ImagenSubida;
import com.reposteria.productos.model.storage.ImagenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.reposteria.productos.apoyo.DatosDePrueba.TORTAS;
import static com.reposteria.productos.apoyo.DatosDePrueba.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Servicio de productos")
class ProductoServiceImplTest {

    @Mock private ProductoRepository productos;
    @Mock private CategoriaRepository categorias;
    @Mock private ImageStorage almacen;
    @Mock private ImagenValidator validador;
    @Mock private ApplicationEventPublisher eventos;
    @Mock private UsuarioActual usuarioActual;
    @Mock private BusquedaProductoStrategy estrategiaQueNoAplica;
    @Mock private BusquedaProductoStrategy estrategiaQueAplica;

    private ProductoServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new ProductoServiceImpl(productos, categorias, new ReglasProducto(),
                List.of(estrategiaQueNoAplica, estrategiaQueAplica), almacen, validador, eventos, usuarioActual,
                new ProductoMapper());
        when(usuarioActual.nombreUsuario()).thenReturn("admin");
        when(categorias.findById(5L)).thenReturn(Optional.of(TORTAS));
        when(productos.saveAndFlush(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static ProductoRequest datos(boolean disponible) {
        return new ProductoRequest("  Torta de zanahoria  ", "Con nueces", 5L, new BigDecimal("45000"), disponible);
    }

    @Nested
    @DisplayName("Registrar (RF-01)")
    class Registrar {

        @Test
        void creaElProductoActivoYPublicaElEvento() {
            ProductoResponse creado = servicio.registrar(datos(true));

            assertThat(creado.nombre()).isEqualTo("Torta de zanahoria");
            assertThat(creado.activo()).isTrue();
            assertThat(creado.disponibilidad()).isTrue();
            assertThat(creado.vendible()).isTrue();
            assertThat(creado.categoria().nombre()).isEqualTo("Tortas");
            verify(eventos).publishEvent(any(ProductoCreadoEvent.class));
        }

        @Test
        void rechazaUnaCategoriaInexistente() {
            when(categorias.findById(99L)).thenReturn(Optional.empty());
            ProductoRequest conCategoriaMala = new ProductoRequest("Torta", null, 99L, BigDecimal.TEN, true);

            assertThatThrownBy(() -> servicio.registrar(conCategoriaMala))
                    .isInstanceOf(DatosInvalidosException.class)
                    .satisfies(e -> assertThat(((DatosInvalidosException) e).getCampo()).isEqualTo("categoriaId"));
            verify(productos, never()).saveAndFlush(any());
        }

        @Test
        void rechazaUnPrecioInvalido() {
            ProductoRequest conPrecioMalo = new ProductoRequest("Torta", null, 5L, new BigDecimal("10.999"), true);

            assertThatThrownBy(() -> servicio.registrar(conPrecioMalo)).isInstanceOf(DatosInvalidosException.class);
            verify(productos, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Consultar (RF-02, RF-05, RF-11)")
    class Consultar {

        @Test
        void obtieneUnProductoExistente() {
            when(productos.findById(1L)).thenReturn(Optional.of(producto(1, "Alfajores", true, true)));

            assertThat(servicio.obtener(1L).nombre()).isEqualTo("Alfajores");
        }

        @Test
        void avisaCuandoElProductoNoExiste() {
            when(productos.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.obtener(404L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("404");
        }

        @Test
        void usaLaPrimeraEstrategiaQueAplica() {
            CriteriosBusqueda criterios = new CriteriosBusqueda("torta", null, null, null);
            Pageable pagina = PageRequest.of(0, 20);
            when(estrategiaQueNoAplica.aplicaA(criterios)).thenReturn(false);
            when(estrategiaQueAplica.aplicaA(criterios)).thenReturn(true);
            when(estrategiaQueAplica.buscar(criterios, pagina))
                    .thenReturn(new PageImpl<>(List.of(producto(1, "Torta negra", true, true)), pagina, 1));

            PaginaResponse<ProductoResponse> resultado = servicio.buscar(criterios, pagina);

            assertThat(resultado.totalElementos()).isEqualTo(1);
            assertThat(resultado.contenido()).extracting(ProductoResponse::nombre).containsExactly("Torta negra");
            verify(estrategiaQueNoAplica, never()).buscar(any(), any());
        }

        @Test
        void sinCriteriosUsaFiltrosVacios() {
            Pageable pagina = PageRequest.of(0, 20);
            when(estrategiaQueAplica.aplicaA(any())).thenReturn(true);
            when(estrategiaQueAplica.buscar(any(), any())).thenReturn(new PageImpl<>(List.of(), pagina, 0));

            assertThat(servicio.buscar(null, pagina).contenido()).isEmpty();
        }

        @Test
        void listaSoloLosDisponibles() {
            Pageable pagina = PageRequest.of(0, 20);
            when(productos.findByActivoTrueAndDisponibilidadTrue(pagina))
                    .thenReturn(new PageImpl<>(List.of(producto(1, "Alfajores", true, true)), pagina, 1));

            assertThat(servicio.listarDisponibles(pagina).contenido()).allMatch(ProductoResponse::vendible);
        }

        @Test
        void resumeLosEstados() {
            when(productos.count()).thenReturn(8L);
            when(productos.countByActivoTrueAndDisponibilidadTrue()).thenReturn(5L);
            when(productos.countByActivoTrueAndDisponibilidadFalse()).thenReturn(2L);
            when(productos.countByActivoFalse()).thenReturn(1L);

            assertThat(servicio.resumen()).isEqualTo(new ResumenVitrinaResponse(8, 5, 2, 1));
        }
    }

    @Nested
    @DisplayName("Editar (RF-03)")
    class Editar {

        @Test
        void actualizaLosDatosYPublicaElEvento() {
            when(productos.findById(1L)).thenReturn(Optional.of(producto(1, "Torta", true, true)));

            ProductoResponse editado = servicio.actualizar(1L, datos(false));

            assertThat(editado.nombre()).isEqualTo("Torta de zanahoria");
            assertThat(editado.disponibilidad()).isFalse();
            ArgumentCaptor<ProductoActualizadoEvent> evento = ArgumentCaptor.forClass(ProductoActualizadoEvent.class);
            verify(eventos).publishEvent(evento.capture());
            assertThat(evento.getValue().operacion()).isEqualTo(TipoOperacion.EDICION);
        }

        @Test
        @DisplayName("No deja marcar como disponible un producto inactivo y registra el rechazo")
        void noPermiteDisponibleSiEstaInactivo() {
            when(productos.findById(8L)).thenReturn(Optional.of(producto(8, "Torta negra", false, false)));

            assertThatThrownBy(() -> servicio.actualizar(8L, datos(true)))
                    .isInstanceOf(ReglaNegocioException.class);
            verify(eventos).publishEvent(any(OperacionRechazadaEvent.class));
            verify(productos, never()).saveAndFlush(any());
        }

        @Test
        void avisaSiNoExiste() {
            when(productos.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.actualizar(404L, datos(true))).isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("Desactivar y reactivar (RF-04, RF-P02)")
    class DesactivarYReactivar {

        @Test
        void desactivarLoDejaInactivoYNoDisponible() {
            when(productos.findById(1L)).thenReturn(Optional.of(producto(1, "Tres leches", true, true)));

            ProductoResponse desactivado = servicio.desactivar(1L);

            assertThat(desactivado.activo()).isFalse();
            assertThat(desactivado.disponibilidad()).isFalse();
            assertThat(desactivado.vendible()).isFalse();
            verify(eventos).publishEvent(any(ProductoDesactivadoEvent.class));
        }

        @Test
        void noDesactivaDosVeces() {
            when(productos.findById(8L)).thenReturn(Optional.of(producto(8, "Torta negra", false, false)));

            assertThatThrownBy(() -> servicio.desactivar(8L)).isInstanceOf(ReglaNegocioException.class);
            verify(eventos).publishEvent(any(OperacionRechazadaEvent.class));
        }

        @Test
        void reactivarLoDevuelveALaCartaSinDisponibilidad() {
            when(productos.findById(8L)).thenReturn(Optional.of(producto(8, "Torta negra", false, false)));

            ProductoResponse reactivado = servicio.reactivar(8L);

            assertThat(reactivado.activo()).isTrue();
            assertThat(reactivado.disponibilidad()).isFalse();
            verify(eventos).publishEvent(any(ProductoReactivadoEvent.class));
        }

        @Test
        void noReactivaUnProductoActivo() {
            when(productos.findById(1L)).thenReturn(Optional.of(producto(1, "Alfajores", true, true)));

            assertThatThrownBy(() -> servicio.reactivar(1L)).isInstanceOf(ReglaNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Cambiar disponibilidad (RF-P03, regla de negocio)")
    class CambiarDisponibilidad {

        @Test
        void marcaComoAgotado() {
            when(productos.findById(1L)).thenReturn(Optional.of(producto(1, "Alfajores", true, true)));

            ProductoResponse resultado = servicio.cambiarDisponibilidad(1L, false);

            assertThat(resultado.disponibilidad()).isFalse();
            assertThat(resultado.activo()).isTrue();
        }

        @Test
        void rechazaMarcarDisponibleUnProductoInactivo() {
            when(productos.findById(8L)).thenReturn(Optional.of(producto(8, "Torta negra", false, false)));

            assertThatThrownBy(() -> servicio.cambiarDisponibilidad(8L, true))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("fuera de carta");
            verify(productos, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Imágenes (RF-10)")
    class Imagenes {

        private final ArchivoRecibido archivo = new ArchivoRecibido(new byte[]{1, 2, 3}, "foto.png");
        private final ImagenSubida imagen = new ImagenSubida(new byte[]{1, 2, 3}, FormatoImagen.PNG);

        @Test
        void guardaLaNuevaYBorraLaAnterior() {
            Producto conFoto = producto(1, "Alfajores", true, true);
            conFoto.cambiarImagen("/uploads/vieja.png");
            when(productos.findById(1L)).thenReturn(Optional.of(conFoto));
            when(validador.validar(archivo)).thenReturn(imagen);
            when(almacen.guardar(imagen)).thenReturn("/uploads/nueva.png");

            ProductoResponse resultado = servicio.actualizarImagen(1L, archivo);

            assertThat(resultado.imagenUrl()).isEqualTo("/uploads/nueva.png");
            verify(almacen).eliminar("/uploads/vieja.png");
        }

        @Test
        void siNoSePuedeGuardarElProductoBorraLaImagenNueva() {
            when(productos.findById(1L)).thenReturn(Optional.of(producto(1, "Alfajores", true, true)));
            when(validador.validar(archivo)).thenReturn(imagen);
            when(almacen.guardar(imagen)).thenReturn("/uploads/nueva.png");
            doThrow(new IllegalStateException("falla de base de datos")).when(productos).saveAndFlush(any());

            assertThatThrownBy(() -> servicio.actualizarImagen(1L, archivo)).isInstanceOf(IllegalStateException.class);
            verify(almacen).eliminar("/uploads/nueva.png");
        }

        @Test
        void unaFallaAlBorrarLaAnteriorNoRompeLaOperacion() {
            Producto conFoto = producto(1, "Alfajores", true, true);
            conFoto.cambiarImagen("/uploads/vieja.png");
            when(productos.findById(1L)).thenReturn(Optional.of(conFoto));
            when(validador.validar(archivo)).thenReturn(imagen);
            when(almacen.guardar(imagen)).thenReturn("/uploads/nueva.png");
            doThrow(new IllegalStateException("disco")).when(almacen).eliminar("/uploads/vieja.png");

            assertThat(servicio.actualizarImagen(1L, archivo).imagenUrl()).isEqualTo("/uploads/nueva.png");
        }
    }
}
