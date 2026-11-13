package com.reposteria.productos.model.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FormatoImagenTest {

    static byte[] jpg() {
        byte[] b = new byte[16];
        b[0] = (byte) 0xFF; b[1] = (byte) 0xD8; b[2] = (byte) 0xFF;
        return b;
    }

    static byte[] png() {
        byte[] b = Arrays.copyOf(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, 16);
        return b;
    }

    static byte[] webp() {
        byte[] b = new byte[16];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, b, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, b, 8, 4);
        return b;
    }

    @Test
    void reconoceLosFormatosPermitidosPorSuFirma() {
        assertThat(FormatoImagen.detectar(jpg())).contains(FormatoImagen.JPG);
        assertThat(FormatoImagen.detectar(png())).contains(FormatoImagen.PNG);
        assertThat(FormatoImagen.detectar(webp())).contains(FormatoImagen.WEBP);
    }

    @Test
    void rechazaArchivosQueNoSonImagenesPermitidas() {
        byte[] gif = Arrays.copyOf("GIF89a".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] pdf = Arrays.copyOf("%PDF-1.7".getBytes(StandardCharsets.US_ASCII), 16);
        assertThat(FormatoImagen.detectar(gif)).isEmpty();
        assertThat(FormatoImagen.detectar(pdf)).isEmpty();
        assertThat(FormatoImagen.detectar(new byte[3])).isEmpty();
        assertThat(FormatoImagen.detectar(null)).isEmpty();
    }

    @Test
    void conoceSuExtensionYTipoDeContenido() {
        assertThat(FormatoImagen.WEBP.extension()).isEqualTo("webp");
        assertThat(FormatoImagen.JPG.tipoContenido()).isEqualTo("image/jpeg");
    }
}
