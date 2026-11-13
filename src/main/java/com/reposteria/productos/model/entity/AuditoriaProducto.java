package com.reposteria.productos.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de actividad (RNF-18): fecha, usuario responsable, operación, producto y resultado.
 * No guarda contraseñas, tokens ni datos sensibles.
 */
@Entity
@Table(name = "auditoria_producto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditoriaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario", nullable = false, length = 50)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "operacion", nullable = false, length = 30)
    private TipoOperacion operacion;

    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "nombre_producto", length = 100)
    private String nombreProducto;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 15)
    private ResultadoOperacion resultado;

    @Column(name = "detalle", length = 255)
    private String detalle;

    public AuditoriaProducto(LocalDateTime fecha, String usuario, TipoOperacion operacion, Long productoId,
                             String nombreProducto, ResultadoOperacion resultado, String detalle) {
        this.fecha = fecha;
        this.usuario = usuario;
        this.operacion = operacion;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.resultado = resultado;
        this.detalle = detalle == null || detalle.length() <= 255 ? detalle : detalle.substring(0, 255);
    }
}
