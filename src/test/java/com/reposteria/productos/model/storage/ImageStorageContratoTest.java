package com.reposteria.productos.model.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LSP: las dos implementaciones de ImageStorage cumplen el mismo contrato,
 * así que el servicio puede usar cualquiera sin cambiar.
 */
@DisplayName("Contrato de ImageStorage (Adapter y LSP)")
class ImageStorageContratoTest {

    @TempDir
    static Path carpeta;

    static Stream<Arguments> implementaciones() {
        Function<Path, ImageStorage> local = LocalImageStorage::new;
        Function<Path, ImageStorage> nube = ruta -> {
            CloudImageService servicio = mock(CloudImageService.class);
            when(servicio.subir(any(), anyString())).thenAnswer(inv ->
                    new ResultadoNube("https://res.cloudinary.com/demo/image/upload/v1/" + inv.getArgument(1) + ".png",
                            inv.getArgument(1)));
            return new CloudImageStorageAdapter(servicio);
        };
        return Stream.of(Arguments.of("local", local), Arguments.of("nube", nube));
    }

    @ParameterizedTest(name = "{0}: guardar devuelve una URL pública y eliminar la acepta")
    @MethodSource("implementaciones")
    void cumpleElContrato(String nombre, Function<Path, ImageStorage> fabrica) {
        ImageStorage almacen = fabrica.apply(carpeta);
        String url = almacen.guardar(new ImagenSubida(FormatoImagenTest.png(), FormatoImagen.PNG));

        assertThat(url).isNotBlank().endsWith(".png");
        assertThatCode(() -> almacen.eliminar(url)).doesNotThrowAnyException();
        assertThatCode(() -> almacen.eliminar(null)).doesNotThrowAnyException();
    }
}
