package com.reposteria.productos.model.rules;

import com.reposteria.productos.exception.DatosInvalidosException;
import com.reposteria.productos.exception.ReglaNegocioException;
import com.reposteria.productos.model.entity.Producto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static com.reposteria.productos.apoyo.DatosDePrueba.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Reglas de negocio del producto")
class ReglasProductoTest {

    private final ReglasProducto reglas = new ReglasProducto();

    @ParameterizedTest(name = "acepta el precio {0}")
    @ValueSource(strings = {"0.01", "6500", "45000.50", "99999999.99"})
    void aceptaPreciosValidos(String precio) {
        assertThatCode(() -> reglas.validarPrecio(new BigDecimal(precio))).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "rechaza el precio {0}")
    @ValueSource(strings = {"0", "-1", "-0.01"})
    void rechazaPreciosNoPositivos(String precio) {
        assertThatThrownBy(() -> reglas.validarPrecio(new BigDecimal(precio)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("mayor que cero");
    }

    @Test
    void rechazaPrecioNulo() {
        assertThatThrownBy(() -> reglas.validarPrecio(null)).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void rechazaMasDeDosDecimales() {
        assertThatThrownBy(() -> reglas.validarPrecio(new BigDecimal("1000.555")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("dos decimales");
    }

    @Test
    void aceptaCerosAlFinalComoDosDecimales() {
        assertThatCode(() -> reglas.validarPrecio(new BigDecimal("1000.500"))).doesNotThrowAnyException();
    }

    @Test
    void rechazaPrecioMayorAlMaximo() {
        assertThatThrownBy(() -> reglas.validarPrecio(new BigDecimal("100000000")))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    @DisplayName("Regla principal: un producto inactivo no puede marcarse disponible")
    void inactivoNoPuedeQuedarDisponible() {
        Producto inactivo = producto(1, "Torta negra", false, false);
        assertThatThrownBy(() -> reglas.validarDisponibilidad(inactivo, true))
                .isInstanceOf(ReglaNegocioException.class)
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCodigo()).isEqualTo(ReglasProducto.PRODUCTO_INACTIVO));
    }

    @Test
    void inactivoSiPuedeQuedarNoDisponible() {
        Producto inactivo = producto(1, "Torta negra", false, false);
        assertThatCode(() -> reglas.validarDisponibilidad(inactivo, false)).doesNotThrowAnyException();
    }

    @Test
    void activoPuedeCambiarDisponibilidad() {
        Producto activo = producto(2, "Tres leches", true, false);
        assertThatCode(() -> reglas.validarDisponibilidad(activo, true)).doesNotThrowAnyException();
    }

    @Test
    void noSeDesactivaDosVeces() {
        Producto inactivo = producto(1, "Torta negra", false, false);
        assertThatThrownBy(() -> reglas.validarDesactivacion(inactivo))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya está fuera de carta");
    }

    @Test
    void noSeReactivaUnProductoActivo() {
        Producto activo = producto(2, "Tres leches", true, true);
        assertThatThrownBy(() -> reglas.validarReactivacion(activo))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya está en la carta");
    }
}
