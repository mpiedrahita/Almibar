package com.reposteria.productos.config;

import com.reposteria.productos.model.storage.CloudImageService;
import com.reposteria.productos.model.storage.CloudImageStorageAdapter;
import com.reposteria.productos.model.storage.ImageStorage;
import com.reposteria.productos.model.storage.LocalImageStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;

/**
 * Elige qué almacenamiento de imágenes usar según la configuración (app.imagenes.proveedor).
 * Es el único lugar que conoce las implementaciones concretas; el resto del sistema
 * solo ve la interfaz ImageStorage.
 */
@Configuration
public class ImagenesConfig {

    @Bean
    public ImageStorage imageStorage(PropiedadesApp propiedades, RestClient.Builder constructorCliente) {
        PropiedadesApp.Imagenes imagenes = propiedades.imagenes();
        String proveedor = imagenes.proveedor().trim().toLowerCase();
        if ("local".equals(proveedor)) {
            return new LocalImageStorage(Path.of(imagenes.directorio()));
        }
        if ("nube".equals(proveedor)) {
            PropiedadesApp.Nube nube = imagenes.nube();
            if (nube == null || vacio(nube.nombreCuenta()) || vacio(nube.claveApi()) || vacio(nube.secretoApi())) {
                throw new IllegalStateException(
                        "Para usar IMAGENES_PROVEEDOR=nube configura CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET.");
            }
            CloudImageService servicio = new CloudImageService(constructorCliente,
                    nube.nombreCuenta(), nube.claveApi(), nube.secretoApi());
            return new CloudImageStorageAdapter(servicio);
        }
        throw new IllegalStateException("IMAGENES_PROVEEDOR debe ser 'local' o 'nube', y llegó: " + imagenes.proveedor());
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
