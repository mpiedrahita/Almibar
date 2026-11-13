package com.reposteria.productos.model.storage;

import com.reposteria.productos.exception.AlmacenamientoException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Patrón Adapter, clase adaptada: cliente del servicio externo Cloudinary.
 * Tiene su propia interfaz (subir con un id público y destruir por id), distinta a
 * ImageStorage. Usa la API REST de Cloudinary con firma SHA-1, sin librerías extra.
 */
public class CloudImageService {

    private final RestClient cliente;
    private final String claveApi;
    private final String secretoApi;

    public CloudImageService(RestClient.Builder constructor, String nombreCuenta, String claveApi, String secretoApi) {
        this.cliente = constructor.baseUrl("https://api.cloudinary.com/v1_1/" + nombreCuenta).build();
        this.claveApi = claveApi;
        this.secretoApi = secretoApi;
    }

    public ResultadoNube subir(byte[] contenido, String idPublico) {
        long momento = System.currentTimeMillis() / 1000;
        MultiValueMap<String, Object> formulario = new LinkedMultiValueMap<>();
        formulario.add("file", new ByteArrayResource(contenido) {
            @Override
            public String getFilename() {
                return "imagen";
            }
        });
        formulario.add("public_id", idPublico);
        formulario.add("timestamp", String.valueOf(momento));
        formulario.add("api_key", claveApi);
        formulario.add("signature", firmar("public_id=" + idPublico + "&timestamp=" + momento));
        try {
            Map<?, ?> respuesta = cliente.post().uri("/image/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formulario)
                    .retrieve()
                    .body(Map.class);
            if (respuesta == null || respuesta.get("secure_url") == null) {
                throw new AlmacenamientoException("El servicio de imágenes no devolvió la dirección de la imagen.", null);
            }
            return new ResultadoNube(String.valueOf(respuesta.get("secure_url")), String.valueOf(respuesta.get("public_id")));
        } catch (RestClientException e) {
            throw new AlmacenamientoException("No se pudo subir la imagen al servicio externo.", e);
        }
    }

    public void destruir(String idPublico) {
        long momento = System.currentTimeMillis() / 1000;
        MultiValueMap<String, Object> formulario = new LinkedMultiValueMap<>();
        formulario.add("public_id", idPublico);
        formulario.add("timestamp", String.valueOf(momento));
        formulario.add("api_key", claveApi);
        formulario.add("signature", firmar("public_id=" + idPublico + "&timestamp=" + momento));
        try {
            cliente.post().uri("/image/destroy")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formulario)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new AlmacenamientoException("No se pudo borrar la imagen del servicio externo.", e);
        }
    }

    private String firmar(String parametros) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] resumen = sha1.digest((parametros + secretoApi).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 no está disponible en esta JVM", e);
        }
    }
}
