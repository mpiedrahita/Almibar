package com.reposteria.productos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Documentación de la API con OpenAPI / Swagger.
 *
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - Especificación OpenAPI (JSON): http://localhost:8080/v3/api-docs
 *
 * springdoc lee los controladores y los DTO y genera la especificación sola. Esta clase solo le agrega
 * lo que no puede deducir del código: títulos en español, el esquema de seguridad con token JWT y
 * ejemplos reales de petición y respuesta (incluidos los errores y la regla de negocio).
 * No cambia el comportamiento de la API.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_TOKEN = "token";
    private static final String JSON = "application/json";
    private static final String FECHA_ERROR = "2026-11-13T15:20:00Z";

    private static final String TAG_AUTENTICACION = "Autenticación";
    private static final String TAG_PRODUCTOS = "Productos";
    private static final String TAG_CATEGORIAS = "Categorías";

    /** Rutas que no piden token (las mismas que deja abiertas SecurityConfig). */
    private static final Set<String> PUBLICAS = Set.of(
            "POST /api/auth/login",
            "GET /api/productos/disponibles",
            "GET /api/categorias");

    @Bean
    public OpenAPI documentacionApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Almíbar · API del módulo de productos")
                        .version("1.0.0")
                        .description("""
                                API REST del módulo de productos de la repostería Almíbar (arquitectura MVC).

                                **Cómo probarla**
                                1. Ejecuta `POST /api/auth/login` con `admin` / `Almibar.2026` (o `consulta` / `Consulta.2026`).
                                2. Copia el valor de `token` de la respuesta.
                                3. Pulsa **Authorize**, pega el token (sin la palabra Bearer) y confirma.

                                **Regla de negocio:** un producto fuera de carta (inactivo) no puede estar disponible.
                                Pruébala con `PATCH /api/productos/8/disponibilidad` y `{"disponible": true}`:
                                el producto 8 (Torta negra) está fuera de carta, así que la API responde **409**.

                                Todos los errores usan el mismo formato: `estado`, `error`, `mensaje`, `codigo` (si aplica),
                                `campos` (si aplica), `ruta` y `fecha`."""))
                .components(new Components().addSecuritySchemes(ESQUEMA_TOKEN, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token que devuelve POST /api/auth/login. Dura 60 minutos.")))
                .tags(List.of(
                        new Tag().name(TAG_AUTENTICACION).description("Inicio de sesión y usuario actual"),
                        new Tag().name(TAG_PRODUCTOS).description("Catálogo de productos: CRUD, búsqueda, disponibilidad e imagen"),
                        new Tag().name(TAG_CATEGORIAS).description("Categorías precargadas (solo consulta)")));
    }

    /** Agrega títulos, seguridad y ejemplos a cada operación que springdoc encontró en los controladores. */
    @Bean
    public OpenApiCustomizer ejemplosDeLaApi() {
        return api -> {
            if (api.getPaths() == null) {
                return;
            }
            api.getPaths().forEach((ruta, item) -> item.readOperationsMap()
                    .forEach((metodo, operacion) -> documentar(metodo.name(), ruta, operacion)));
        };
    }

    // ------------------------------------------------------------------
    // Documentación de cada endpoint
    // ------------------------------------------------------------------

    private static void documentar(String metodo, String ruta, Operation op) {
        if (!ruta.startsWith("/api/")) {
            return;
        }
        String clave = metodo + " " + ruta;

        if (ruta.startsWith("/api/auth")) {
            op.setTags(List.of(TAG_AUTENTICACION));
        } else if (ruta.startsWith("/api/categorias")) {
            op.setTags(List.of(TAG_CATEGORIAS));
        } else {
            op.setTags(List.of(TAG_PRODUCTOS));
        }

        if (!PUBLICAS.contains(clave)) {
            op.setSecurity(List.of(new SecurityRequirement().addList(ESQUEMA_TOKEN)));
            error(op, "401", "No hay token, o venció o fue alterado", ruta.replace("{id}", "8"),
                    "Inicia sesión para hacer esta operación.", null, null);
            if (!"GET".equals(metodo)) {
                error(op, "403", "El usuario no es ADMIN (por ejemplo, el usuario consulta)", ruta.replace("{id}", "8"),
                        "Tu usuario no tiene permiso para modificar productos.", null, null);
            }
        }
        if (ruta.contains("{id}")) {
            error(op, "404", "No existe un producto con ese id", ruta.replace("{id}", "99"),
                    "No existe un producto con el id 99.", null, null);
        }

        switch (clave) {
            case "POST /api/auth/login" -> {
                op.setSummary("Iniciar sesión");
                op.setDescription("Recibe usuario y contraseña y devuelve un token JWT que dura 60 minutos. "
                        + "Si falla, no dice si el error fue el usuario o la contraseña.");
                cuerpo(op, mapa("usuario", "admin", "contrasena", "Almibar.2026"));
                exito(op, "200", "Sesión iniciada", mapa(
                        "token", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbCI6IkFETUlOIn0.firma",
                        "tipo", "Bearer",
                        "expiraEnSegundos", 3600,
                        "usuario", mapa("usuario", "admin", "nombre", "Ana", "rol", "ADMIN")));
                error(op, "400", "Falta el usuario o la contraseña", ruta, "Revisa los campos marcados.", null,
                        List.of(mapa("campo", "contrasena", "mensaje", "Escribe tu contraseña.")));
                error(op, "401", "Usuario o contraseña incorrectos", ruta, "Usuario o contraseña incorrectos.", null, null);
            }
            case "GET /api/auth/yo" -> {
                op.setSummary("Ver el usuario de la sesión");
                exito(op, "200", "Usuario del token", mapa("usuario", "admin", "nombre", "Ana", "rol", "ADMIN"));
            }
            case "GET /api/categorias" -> {
                op.setSummary("Listar categorías (público)");
                exito(op, "200", "Las cinco categorías precargadas", List.of(
                        mapa("id", 1, "nombre", "Tortas"),
                        mapa("id", 2, "nombre", "Tartas"),
                        mapa("id", 3, "nombre", "Postres fríos"),
                        mapa("id", 4, "nombre", "Galletas y alfajores"),
                        mapa("id", 5, "nombre", "Panadería dulce")));
            }
            case "GET /api/productos" -> {
                op.setSummary("Buscar y listar productos");
                op.setDescription("Todos los filtros son opcionales y se pueden combinar: nombre (contiene, sin "
                        + "distinguir mayúsculas), categoriaId, disponible y activo. Paginado: 20 por defecto, máximo 100. "
                        + "Usa el patrón Strategy para elegir la búsqueda.");
                exito(op, "200", "Una página de resultados", pagina(alfajores()));
                error(op, "400", "Parámetros fuera de rango (por ejemplo size=500)", ruta,
                        "Revisa los parámetros de la consulta.", null,
                        List.of(mapa("campo", "consulta", "mensaje", "El tamaño máximo de página es 100.")));
            }
            case "GET /api/productos/disponibles" -> {
                op.setSummary("Listar productos que se pueden vender (público)");
                op.setDescription("Solo productos activos y disponibles. Es la consulta que usaría el módulo de pedidos.");
                exito(op, "200", "Una página de productos vendibles", pagina(alfajores()));
            }
            case "GET /api/productos/resumen" -> {
                op.setSummary("Contar productos por estado");
                exito(op, "200", "Conteo para las pestañas de la vitrina",
                        mapa("total", 8, "enVitrina", 5, "agotados", 2, "fueraDeCarta", 1));
            }
            case "GET /api/productos/{id}" -> {
                op.setSummary("Ver un producto");
                exito(op, "200", "El producto", alfajores());
            }
            case "POST /api/productos" -> {
                op.setSummary("Crear un producto (solo ADMIN)");
                op.setDescription("El producto queda activo. Responde 201 con la cabecera Location.");
                cuerpo(op, nuevoProducto());
                exito(op, "201", "Producto creado", producto(9, "Brownie con arequipe",
                        "Brownie de chocolate con una capa de arequipe.", 5, "Panadería dulce", "8500.00",
                        true, true, null));
                error(op, "400", "Campos obligatorios vacíos o con formato incorrecto", ruta,
                        "Revisa los campos marcados.", null, List.of(
                                mapa("campo", "nombre", "mensaje", "Escribe el nombre del producto."),
                                mapa("campo", "precio", "mensaje", "El precio debe ser mayor que cero.")));
                error(op, "422", "La categoría no existe", ruta,
                        "La categoría elegida no existe.", null,
                        List.of(mapa("campo", "categoriaId", "mensaje", "La categoría elegida no existe.")));
            }
            case "PUT /api/productos/{id}" -> {
                op.setSummary("Editar un producto (solo ADMIN)");
                op.setDescription("Cambia nombre, descripción, categoría, precio y disponibilidad. "
                        + "El id, el estado y las fechas no se editan.");
                cuerpo(op, nuevoProducto());
                exito(op, "200", "Producto actualizado", producto(9, "Brownie con arequipe",
                        "Brownie de chocolate con una capa de arequipe.", 5, "Panadería dulce", "8500.00",
                        true, true, null));
                error(op, "400", "Campos con formato incorrecto", "/api/productos/9",
                        "Revisa los campos marcados.", null,
                        List.of(mapa("campo", "precio", "mensaje", "El precio admite máximo 8 cifras y dos decimales.")));
                error(op, "409", "Regla de negocio: se intentó dejar disponible un producto fuera de carta",
                        "/api/productos/8",
                        "«Torta negra» está fuera de carta. Vuelve a ponerlo en la carta antes de marcarlo como disponible.",
                        "PRODUCTO_INACTIVO", null);
                error(op, "422", "La categoría no existe", "/api/productos/9",
                        "La categoría elegida no existe.", null,
                        List.of(mapa("campo", "categoriaId", "mensaje", "La categoría elegida no existe.")));
            }
            case "DELETE /api/productos/{id}" -> {
                op.setSummary("Sacar un producto de la carta (eliminación lógica, solo ADMIN)");
                op.setDescription("No borra el producto: lo deja con activo = false y disponibilidad = false. "
                        + "Se conserva su historial.");
                exito(op, "200", "Producto fuera de carta", producto(1, "Alfajores de pistacho",
                        "Galleta de mantequilla, relleno de pistacho y baño de chocolate blanco.", 4,
                        "Galletas y alfajores", "6500.00", false, false, "/img/productos/alfajores-pistacho.webp"));
                error(op, "409", "El producto ya estaba fuera de carta", "/api/productos/8",
                        "«Torta negra» ya está fuera de carta.", "PRODUCTO_YA_INACTIVO", null);
            }
            case "PATCH /api/productos/{id}/reactivar" -> {
                op.setSummary("Volver a poner un producto en la carta (solo ADMIN)");
                op.setDescription("Queda activo pero no disponible, hasta que la administradora cambie la disponibilidad.");
                exito(op, "200", "Producto de nuevo en la carta", producto(8, "Torta negra",
                        "Receta de temporada con frutas maceradas en vino.", 1, "Tortas", "65000.00",
                        false, true, null));
                error(op, "409", "El producto ya estaba en la carta", "/api/productos/1/reactivar",
                        "«Alfajores de pistacho» ya está en la carta.", "PRODUCTO_YA_ACTIVO", null);
            }
            case "PATCH /api/productos/{id}/disponibilidad" -> {
                op.setSummary("Cambiar la disponibilidad (aplica la regla de negocio, solo ADMIN)");
                op.setDescription("Regla: un producto fuera de carta no puede quedar disponible. "
                        + "Pruébala con el id 8 (Torta negra, fuera de carta) y disponible = true: responde 409.");
                cuerpo(op, mapa("disponible", true));
                exito(op, "200", "Disponibilidad cambiada", alfajores());
                error(op, "400", "Falta el campo disponible", "/api/productos/1/disponibilidad",
                        "Revisa los campos marcados.", null,
                        List.of(mapa("campo", "disponible", "mensaje", "Indica si el producto queda disponible.")));
                error(op, "409", "Regla de negocio: el producto está fuera de carta", "/api/productos/8/disponibilidad",
                        "«Torta negra» está fuera de carta. Vuelve a ponerlo en la carta antes de marcarlo como disponible.",
                        "PRODUCTO_INACTIVO", null);
            }
            case "POST /api/productos/{id}/imagen" -> {
                op.setSummary("Subir o reemplazar la foto (solo ADMIN)");
                op.setDescription("Formulario multipart con el campo archivo. Acepta JPG, PNG o WEBP de máximo 5 MB; "
                        + "se revisa el contenido real del archivo. Al reemplazarla se borra la anterior.");
                exito(op, "200", "Foto guardada", alfajores());
                error(op, "413", "La imagen pesa más de 5 MB", "/api/productos/1/imagen",
                        "La imagen pesa más de 5 MB. Usa una más liviana.", null, null);
                error(op, "422", "El archivo no es una imagen JPG, PNG o WEBP", "/api/productos/1/imagen",
                        "Ese archivo no es una imagen JPG, PNG o WEBP.", null,
                        List.of(mapa("campo", "archivo", "mensaje", "Ese archivo no es una imagen JPG, PNG o WEBP.")));
            }
            default -> {
                // Cualquier endpoint nuevo aparece igual, con lo que springdoc deduce del código.
            }
        }
    }

    // ------------------------------------------------------------------
    // Ejemplos
    // ------------------------------------------------------------------

    private static Map<String, Object> nuevoProducto() {
        return mapa("nombre", "Brownie con arequipe",
                "descripcion", "Brownie de chocolate con una capa de arequipe.",
                "categoriaId", 5,
                "precio", new BigDecimal("8500.00"),
                "disponible", true);
    }

    private static Map<String, Object> alfajores() {
        return producto(1, "Alfajores de pistacho",
                "Galleta de mantequilla, relleno de pistacho y baño de chocolate blanco.", 4,
                "Galletas y alfajores", "6500.00", true, true, "/img/productos/alfajores-pistacho.webp");
    }

    private static Map<String, Object> producto(long id, String nombre, String descripcion, long categoriaId,
                                                String categoria, String precio, boolean disponible, boolean activo,
                                                String imagenUrl) {
        return mapa("id", id,
                "nombre", nombre,
                "descripcion", descripcion,
                "categoria", mapa("id", categoriaId, "nombre", categoria),
                "precio", new BigDecimal(precio),
                "disponibilidad", disponible,
                "activo", activo,
                "vendible", disponible && activo,
                "imagenUrl", imagenUrl,
                "fechaRegistro", "2026-11-01T09:00:00",
                "fechaActualizacion", "2026-11-13T10:15:00");
    }

    private static Map<String, Object> pagina(Map<String, Object> producto) {
        return mapa("contenido", List.of(producto),
                "pagina", 0,
                "tamano", 20,
                "totalElementos", 1,
                "totalPaginas", 1);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static void cuerpo(Operation op, Object ejemplo) {
        if (op.getRequestBody() == null || op.getRequestBody().getContent() == null) {
            return;
        }
        op.getRequestBody().getContent().values().forEach(tipo -> tipo.setExample(ejemplo));
    }

    private static void exito(Operation op, String codigo, String descripcion, Object ejemplo) {
        ApiResponses respuestas = respuestas(op);
        ApiResponse respuesta = respuestas.get(codigo);
        if (respuesta == null) {
            // springdoc documenta 200 por defecto; si el código real es otro (201), se usa ese.
            respuesta = respuestas.remove("200");
            if (respuesta == null) {
                respuesta = new ApiResponse();
            }
            respuestas.addApiResponse(codigo, respuesta);
        }
        respuesta.setDescription(descripcion);
        if (respuesta.getContent() == null || respuesta.getContent().isEmpty()) {
            respuesta.setContent(new Content().addMediaType(JSON, new MediaType()));
        }
        respuesta.getContent().values().forEach(tipo -> tipo.setExample(ejemplo));
    }

    private static void error(Operation op, String codigo, String descripcion, String ruta, String mensaje,
                              String codigoNegocio, List<Map<String, Object>> campos) {
        int estado = Integer.parseInt(codigo);
        Map<String, Object> cuerpo = mapa(
                "estado", estado,
                "error", HttpStatus.valueOf(estado).getReasonPhrase(),
                "mensaje", mensaje);
        if (codigoNegocio != null) {
            cuerpo.put("codigo", codigoNegocio);
        }
        if (campos != null) {
            cuerpo.put("campos", campos);
        }
        cuerpo.put("ruta", ruta);
        cuerpo.put("fecha", FECHA_ERROR);
        respuestas(op).addApiResponse(codigo, new ApiResponse()
                .description(descripcion)
                .content(new Content().addMediaType(JSON, new MediaType().example(cuerpo))));
    }

    private static ApiResponses respuestas(Operation op) {
        if (op.getResponses() == null) {
            op.setResponses(new ApiResponses());
        }
        return op.getResponses();
    }

    private static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            resultado.put((String) pares[i], pares[i + 1]);
        }
        return resultado;
    }
}
