package com.reposteria.productos.model.storage;

import com.reposteria.productos.config.PropiedadesApp;
import com.reposteria.productos.exception.ArchivoDemasiadoGrandeException;
import com.reposteria.productos.exception.DatosInvalidosException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Valida tamaño y formato real de las imágenes (RNF-21). */
@Component
public class ImagenValidator {

    private final long tamanoMaximoBytes;

    @Autowired
    public ImagenValidator(PropiedadesApp propiedades) {
        this(propiedades.imagenes().tamanoMaximoBytes());
    }

    /** Constructor para pruebas unitarias. */
    ImagenValidator(long tamanoMaximoBytes) {
        this.tamanoMaximoBytes = tamanoMaximoBytes;
    }

    public ImagenSubida validar(ArchivoRecibido archivo) {
        if (archivo == null || archivo.contenido() == null || archivo.contenido().length == 0) {
            throw new DatosInvalidosException("archivo", "Elige una imagen para subir.");
        }
        if (archivo.contenido().length > tamanoMaximoBytes) {
            throw new ArchivoDemasiadoGrandeException("La imagen pesa más de " + (tamanoMaximoBytes / (1024 * 1024))
                    + " MB. Usa una más liviana.");
        }
        FormatoImagen formato = FormatoImagen.detectar(archivo.contenido())
                .orElseThrow(() -> new DatosInvalidosException("archivo",
                        "Ese archivo no es una imagen JPG, PNG o WEBP."));
        return new ImagenSubida(archivo.contenido(), formato);
    }
}
