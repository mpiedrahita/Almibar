package com.reposteria.productos.model.storage;

import java.util.Optional;

/**
 * Formatos de imagen permitidos (RNF-21). El formato se reconoce por la firma
 * de los primeros bytes del archivo, no por la extensión, que se puede falsificar.
 */
public enum FormatoImagen {

    JPG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp");

    private final String extension;
    private final String tipoContenido;

    FormatoImagen(String extension, String tipoContenido) {
        this.extension = extension;
        this.tipoContenido = tipoContenido;
    }

    public String extension() {
        return extension;
    }

    public String tipoContenido() {
        return tipoContenido;
    }

    public static Optional<FormatoImagen> detectar(byte[] contenido) {
        if (contenido == null || contenido.length < 12) {
            return Optional.empty();
        }
        if (byteA(contenido, 0) == 0xFF && byteA(contenido, 1) == 0xD8 && byteA(contenido, 2) == 0xFF) {
            return Optional.of(JPG);
        }
        int[] firmaPng = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        boolean esPng = true;
        for (int i = 0; i < firmaPng.length; i++) {
            if (byteA(contenido, i) != firmaPng[i]) {
                esPng = false;
                break;
            }
        }
        if (esPng) {
            return Optional.of(PNG);
        }
        boolean esRiff = contenido[0] == 'R' && contenido[1] == 'I' && contenido[2] == 'F' && contenido[3] == 'F';
        boolean esWebp = contenido[8] == 'W' && contenido[9] == 'E' && contenido[10] == 'B' && contenido[11] == 'P';
        if (esRiff && esWebp) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static int byteA(byte[] contenido, int posicion) {
        return contenido[posicion] & 0xFF;
    }
}
