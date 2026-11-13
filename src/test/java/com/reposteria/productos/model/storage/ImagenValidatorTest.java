package com.reposteria.productos.model.storage;

import com.reposteria.productos.exception.ArchivoDemasiadoGrandeException;
import com.reposteria.productos.exception.DatosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Validación de imágenes (RNF-21)")
class ImagenValidatorTest {

    private final ImagenValidator validador = new ImagenValidator(5L * 1024 * 1024);

    @Test
    void aceptaUnPngValido() {
        ImagenSubida imagen = validador.validar(new ArchivoRecibido(FormatoImagenTest.png(), "foto.png"));
        assertThat(imagen.formato()).isEqualTo(FormatoImagen.PNG);
    }

    @Test
    void rechazaUnArchivoVacio() {
        assertThatThrownBy(() -> validador.validar(new ArchivoRecibido(new byte[0], "vacio.png")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> validador.validar(null)).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void rechazaUnArchivoDeMasDeCincoMegas() {
        byte[] grande = Arrays.copyOf(FormatoImagenTest.jpg(), 5 * 1024 * 1024 + 1);
        assertThatThrownBy(() -> validador.validar(new ArchivoRecibido(grande, "grande.jpg")))
                .isInstanceOf(ArchivoDemasiadoGrandeException.class);
    }

    @Test
    @DisplayName("Rechaza un archivo con extensión de imagen pero contenido de otro tipo")
    void rechazaUnArchivoDisfrazado() {
        byte[] pdf = Arrays.copyOf("%PDF-1.7".getBytes(StandardCharsets.US_ASCII), 32);
        assertThatThrownBy(() -> validador.validar(new ArchivoRecibido(pdf, "foto.jpg")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("JPG, PNG o WEBP");
    }
}
