package com.reposteria.productos.model.rules;

import com.reposteria.productos.exception.DatosInvalidosException;
import com.reposteria.productos.exception.ReglaNegocioException;
import com.reposteria.productos.model.entity.Producto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Reglas de negocio del módulo de productos, en un solo lugar (DRY).
 * El servicio las usa antes de cambiar un producto; no dependen de HTTP ni de la base de datos.
 *
 * Regla principal (no trivial): un producto inactivo ("fuera de carta") no puede estar
 * disponible para la venta. Al desactivarlo deja de estar disponible, y mientras siga
 * inactivo no se puede marcar como disponible ni al editarlo ni con el cambio rápido.
 * La base de datos también la protege con una restricción CHECK.
 */
@Component
public class ReglasProducto {

    public static final String PRODUCTO_INACTIVO = "PRODUCTO_INACTIVO";
    public static final String YA_INACTIVO = "PRODUCTO_YA_INACTIVO";
    public static final String YA_ACTIVO = "PRODUCTO_YA_ACTIVO";

    /** Precio máximo que admite DECIMAL(10,2). */
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

    /** RNF-10: el precio debe ser mayor que cero y tener máximo dos decimales. */
    public void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new DatosInvalidosException("precio", "El precio debe ser mayor que cero.");
        }
        if (precio.stripTrailingZeros().scale() > 2) {
            throw new DatosInvalidosException("precio", "El precio admite máximo dos decimales.");
        }
        if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            throw new DatosInvalidosException("precio", "El precio no puede superar $ 99.999.999,99.");
        }
    }

    /** Regla principal: solo un producto activo puede quedar disponible. */
    public void validarDisponibilidad(Producto producto, boolean disponible) {
        if (disponible && !producto.isActivo()) {
            throw new ReglaNegocioException(PRODUCTO_INACTIVO,
                    "«" + producto.getNombre() + "» está fuera de carta. Vuelve a ponerlo en la carta antes de marcarlo como disponible.");
        }
    }

    public void validarDesactivacion(Producto producto) {
        if (!producto.isActivo()) {
            throw new ReglaNegocioException(YA_INACTIVO, "«" + producto.getNombre() + "» ya está fuera de carta.");
        }
    }

    public void validarReactivacion(Producto producto) {
        if (producto.isActivo()) {
            throw new ReglaNegocioException(YA_ACTIVO, "«" + producto.getNombre() + "» ya está en la carta.");
        }
    }
}
