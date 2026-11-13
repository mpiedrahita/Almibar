package com.reposteria.productos.model.storage;

import java.util.UUID;

/**
 * Patrón Adapter: hace que CloudImageService se pueda usar como ImageStorage.
 * Traduce "guardar una imagen y devolver su URL" a "subir con un id público", y
 * "eliminar por URL" a "destruir por id público".
 */
public class CloudImageStorageAdapter implements ImageStorage {

    static final String CARPETA = "almibar/productos/";

    private final CloudImageService servicioNube;

    public CloudImageStorageAdapter(CloudImageService servicioNube) {
        this.servicioNube = servicioNube;
    }

    @Override
    public String guardar(ImagenSubida imagen) {
        ResultadoNube resultado = servicioNube.subir(imagen.contenido(), CARPETA + UUID.randomUUID());
        return resultado.urlSegura();
    }

    @Override
    public void eliminar(String url) {
        String idPublico = idPublicoDesde(url);
        if (idPublico != null) {
            servicioNube.destruir(idPublico);
        }
    }

    /**
     * De https://res.cloudinary.com/cuenta/image/upload/v1712/almibar/productos/abc.webp
     * obtiene almibar/productos/abc. Devuelve null si la URL no es de este proveedor.
     */
    static String idPublicoDesde(String url) {
        if (url == null) {
            return null;
        }
        int inicio = url.indexOf("/upload/");
        if (inicio < 0) {
            return null;
        }
        String ruta = url.substring(inicio + "/upload/".length());
        if (ruta.matches("v\\d+/.*")) {
            ruta = ruta.substring(ruta.indexOf('/') + 1);
        }
        int punto = ruta.lastIndexOf('.');
        return punto > 0 ? ruta.substring(0, punto) : ruta;
    }
}
