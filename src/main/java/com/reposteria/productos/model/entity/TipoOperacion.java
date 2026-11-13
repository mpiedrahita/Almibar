package com.reposteria.productos.model.entity;

/** Operaciones administrativas que quedan en la auditoría (RNF-18). */
public enum TipoOperacion {
    CREACION,
    EDICION,
    DESACTIVACION,
    REACTIVACION,
    CAMBIO_DISPONIBILIDAD,
    CAMBIO_IMAGEN
}
