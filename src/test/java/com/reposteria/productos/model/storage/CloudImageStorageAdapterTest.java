package com.reposteria.productos.model.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Adapter de Cloudinary")
class CloudImageStorageAdapterTest {

    private final CloudImageService servicio = mock(CloudImageService.class);
    private final CloudImageStorageAdapter adaptador = new CloudImageStorageAdapter(servicio);

    @Test
    void guardarSubeConUnIdPublicoYDevuelveLaUrlSegura() {
        when(servicio.subir(any(), anyString()))
                .thenReturn(new ResultadoNube("https://res.cloudinary.com/demo/image/upload/v9/almibar/productos/x.png", "almibar/productos/x"));

        String url = adaptador.guardar(new ImagenSubida(FormatoImagenTest.png(), FormatoImagen.PNG));

        assertThat(url).startsWith("https://res.cloudinary.com/");
        verify(servicio).subir(any(), startsWith(CloudImageStorageAdapter.CARPETA));
    }

    @Test
    void eliminarTraduceLaUrlAlIdPublico() {
        adaptador.eliminar("https://res.cloudinary.com/demo/image/upload/v1712345/almibar/productos/abc.webp");

        verify(servicio).destruir("almibar/productos/abc");
    }

    @Test
    void noLlamaAlServicioConUrlsAjenas() {
        adaptador.eliminar("/uploads/local.png");
        adaptador.eliminar(null);

        verify(servicio, never()).destruir(anyString());
    }

    @Test
    void extraeElIdConYSinVersion() {
        assertThat(CloudImageStorageAdapter.idPublicoDesde("https://res.cloudinary.com/c/image/upload/almibar/productos/a.png"))
                .isEqualTo("almibar/productos/a");
        assertThat(CloudImageStorageAdapter.idPublicoDesde("https://res.cloudinary.com/c/image/upload/v1/almibar/productos/b"))
                .isEqualTo("almibar/productos/b");
    }
}
