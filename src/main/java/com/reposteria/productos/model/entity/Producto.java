package com.reposteria.productos.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Producto de repostería (sección 3 del documento).
 *
 * Encapsulamiento (sección 8.4): no tiene setters. Su estado solo cambia mediante
 * operaciones con significado (registrar, actualizar, desactivar, reactivar...).
 * Las reglas de negocio se validan antes en el servicio, con ReglasProducto.
 */
@Entity
@Table(name = "producto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long idProducto;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    /** Si se puede vender en este momento (por ejemplo, false cuando se agota). */
    @Column(name = "disponibilidad", nullable = false)
    private boolean disponibilidad;

    /** Si sigue habilitado en el catálogo. Desactivar = eliminación lógica (RF-04). */
    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    /** Bloqueo optimista: evita que dos administradores se pisen los cambios. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    private Producto(String nombre, String descripcion, Categoria categoria, BigDecimal precio, boolean disponibilidad) {
        asignarDatos(nombre, descripcion, categoria, precio, disponibilidad);
        this.activo = true;
    }

    /** Crea un producto nuevo; siempre entra activo al catálogo. */
    public static Producto registrar(String nombre, String descripcion, Categoria categoria,
                                     BigDecimal precio, boolean disponibilidad) {
        return new Producto(nombre, descripcion, categoria, precio, disponibilidad);
    }

    /** Modifica los datos editables (RF-03). El id, el estado y las fechas no se editan. */
    public void actualizar(String nombre, String descripcion, Categoria categoria,
                           BigDecimal precio, boolean disponibilidad) {
        asignarDatos(nombre, descripcion, categoria, precio, disponibilidad);
    }

    /** Lo saca del catálogo sin borrarlo; un producto inactivo nunca queda disponible. */
    public void desactivar() {
        this.activo = false;
        this.disponibilidad = false;
    }

    /** Lo devuelve al catálogo; sigue sin estar disponible hasta que se indique. */
    public void reactivar() {
        this.activo = true;
    }

    public void cambiarDisponibilidad(boolean disponible) {
        this.disponibilidad = disponible;
    }

    public void cambiarImagen(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    /** RF-08: se puede vender solo si está activo y disponible a la vez. */
    public boolean esVendible() {
        return activo && disponibilidad;
    }

    private void asignarDatos(String nombre, String descripcion, Categoria categoria,
                              BigDecimal precio, boolean disponibilidad) {
        this.nombre = nombre == null ? null : nombre.trim();
        this.descripcion = (descripcion == null || descripcion.isBlank()) ? null : descripcion.trim();
        this.categoria = categoria;
        this.precio = precio;
        this.disponibilidad = disponibilidad;
    }

    /** RF-12: fechas automáticas. */
    @PrePersist
    void alRegistrar() {
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaRegistro = ahora;
        this.fechaActualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
