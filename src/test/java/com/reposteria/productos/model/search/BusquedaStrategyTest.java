package com.reposteria.productos.model.search;

import com.reposteria.productos.exception.RecursoNoEncontradoException;
import com.reposteria.productos.model.repository.CategoriaRepository;
import com.reposteria.productos.model.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Estrategias de búsqueda (Strategy)")
class BusquedaStrategyTest {

    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final Pageable pagina = PageRequest.of(0, 20);

    private final BusquedaPorNombre porNombre = new BusquedaPorNombre(productos);
    private final BusquedaPorCategoria porCategoria = new BusquedaPorCategoria(productos, categorias);
    private final BusquedaCombinada combinada = new BusquedaCombinada(productos);

    @Test
    void cadaEstrategiaSabeQueCriteriosResuelve() {
        CriteriosBusqueda soloNombre = new CriteriosBusqueda(" torta ", null, null, null);
        CriteriosBusqueda soloCategoria = new CriteriosBusqueda(null, 5L, null, null);
        CriteriosBusqueda nombreYCategoria = new CriteriosBusqueda("torta", 5L, null, null);
        CriteriosBusqueda nombreYEstado = new CriteriosBusqueda("torta", null, true, null);

        assertThat(porNombre.aplicaA(soloNombre)).isTrue();
        assertThat(porNombre.aplicaA(nombreYCategoria)).isFalse();
        assertThat(porNombre.aplicaA(nombreYEstado)).isFalse();
        assertThat(porCategoria.aplicaA(soloCategoria)).isTrue();
        assertThat(porCategoria.aplicaA(nombreYCategoria)).isFalse();
        assertThat(combinada.aplicaA(nombreYCategoria)).isTrue();
        assertThat(combinada.aplicaA(CriteriosBusqueda.vacios())).isTrue();
    }

    @Test
    void porNombreBuscaSinEspaciosSobrantes() {
        when(productos.findByNombreContainingIgnoreCase("torta", pagina)).thenReturn(Page.empty());

        porNombre.buscar(new CriteriosBusqueda("  torta ", null, null, null), pagina);

        verify(productos).findByNombreContainingIgnoreCase("torta", pagina);
    }

    @Test
    void porCategoriaFallaSiLaCategoriaNoExiste() {
        when(categorias.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> porCategoria.buscar(new CriteriosBusqueda(null, 99L, null, null), pagina))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void porCategoriaBuscaSiExiste() {
        when(categorias.existsById(5L)).thenReturn(true);
        when(productos.findByCategoria_IdCategoria(5L, pagina)).thenReturn(Page.empty());

        porCategoria.buscar(new CriteriosBusqueda(null, 5L, null, null), pagina);

        verify(productos).findByCategoria_IdCategoria(5L, pagina);
    }

    @Test
    @SuppressWarnings("unchecked")
    void combinadaUsaUnaConsultaDinamica() {
        when(productos.findAll(any(Specification.class), eq(pagina))).thenReturn(Page.empty());

        combinada.buscar(new CriteriosBusqueda("torta", 5L, true, true), pagina);

        verify(productos).findAll(any(Specification.class), eq(pagina));
    }

    @Test
    void normalizaElNombre() {
        assertThat(new CriteriosBusqueda("  TORTA Negra ", null, null, null).nombreNormalizado()).isEqualTo("torta negra");
        assertThat(CriteriosBusqueda.vacios().nombreNormalizado()).isEmpty();
        assertThat(new CriteriosBusqueda("   ", null, null, null).tieneNombre()).isFalse();
    }
}
