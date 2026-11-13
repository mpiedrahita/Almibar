package com.reposteria.productos.model.storage;

import com.reposteria.productos.exception.AlmacenamientoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Guarda las imágenes en una carpeta del servidor (RNF-22). En la base de datos
 * solo queda la ruta pública, por ejemplo /uploads/2f1c...webp.
 */
public class LocalImageStorage implements ImageStorage {

    public static final String PREFIJO_URL = "/uploads/";

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorage.class);

    private final Path carpeta;

    public LocalImageStorage(Path carpeta) {
        this.carpeta = carpeta.toAbsolutePath().normalize();
    }

    @Override
    public String guardar(ImagenSubida imagen) {
        String nombreArchivo = UUID.randomUUID() + "." + imagen.formato().extension();
        try {
            Files.createDirectories(carpeta);
            Files.write(carpeta.resolve(nombreArchivo), imagen.contenido());
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo guardar la imagen. Inténtalo de nuevo.", e);
        }
        return PREFIJO_URL + nombreArchivo;
    }

    @Override
    public void eliminar(String url) {
        if (url == null || !url.startsWith(PREFIJO_URL)) {
            return;
        }
        Path archivo = carpeta.resolve(url.substring(PREFIJO_URL.length())).normalize();
        if (!archivo.startsWith(carpeta)) {
            log.warn("Se intentó borrar un archivo fuera de la carpeta de imágenes");
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo borrar la imagen anterior {}", archivo.getFileName());
        }
    }

    public Path carpeta() {
        return carpeta;
    }
}
