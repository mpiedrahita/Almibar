package com.reposteria.productos.model.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalImageStorageTest {

    @TempDir
    Path carpeta;

    @Test
    void guardaElArchivoYDevuelveSuRutaPublica() throws Exception {
        LocalImageStorage almacen = new LocalImageStorage(carpeta);

        String url = almacen.guardar(new ImagenSubida(FormatoImagenTest.webp(), FormatoImagen.WEBP));

        assertThat(url).startsWith("/uploads/").endsWith(".webp");
        Path archivo = carpeta.resolve(url.substring("/uploads/".length()));
        assertThat(Files.readAllBytes(archivo)).isEqualTo(FormatoImagenTest.webp());
    }

    @Test
    void eliminaElArchivo() {
        LocalImageStorage almacen = new LocalImageStorage(carpeta);
        String url = almacen.guardar(new ImagenSubida(FormatoImagenTest.png(), FormatoImagen.PNG));

        almacen.eliminar(url);

        assertThat(carpeta.resolve(url.substring("/uploads/".length()))).doesNotExist();
    }

    @Test
    void noBorraArchivosFueraDeSuCarpeta() throws Exception {
        Path ajeno = Files.writeString(carpeta.getParent().resolve("ajeno-" + System.nanoTime() + ".txt"), "no borrar");
        LocalImageStorage almacen = new LocalImageStorage(carpeta);

        almacen.eliminar("/uploads/../" + ajeno.getFileName());

        assertThat(ajeno).exists();
        Files.deleteIfExists(ajeno);
    }

    @Test
    void ignoraUrlsQueNoSonLocales() {
        LocalImageStorage almacen = new LocalImageStorage(carpeta);
        almacen.eliminar("/img/productos/alfajores-pistacho.webp");
        almacen.eliminar("https://res.cloudinary.com/demo/image/upload/v1/a.png");
        assertThat(almacen.carpeta()).isEqualTo(carpeta.toAbsolutePath().normalize());
    }
}
