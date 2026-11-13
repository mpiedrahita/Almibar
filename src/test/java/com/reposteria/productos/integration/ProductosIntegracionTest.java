package com.reposteria.productos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reposteria.productos.model.entity.AuditoriaProducto;
import com.reposteria.productos.model.entity.ResultadoOperacion;
import com.reposteria.productos.model.repository.AuditoriaProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración contra un MySQL real (Testcontainers): migraciones de Flyway,
 * inicio de sesión con BCrypt, CRUD completo, regla de negocio, restricciones de la base
 * de datos y auditoría. Necesitan Docker; si Docker no está corriendo, se omiten.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integración con MySQL")
class ProductosIntegracionTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuditoriaProductoRepository auditoria;

    private String tokenAdmin;

    @BeforeEach
    void iniciarSesion() throws Exception {
        tokenAdmin = "Bearer " + login("admin", "Almibar.2026");
    }

    private String login(String usuario, String clave) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\",\"contrasena\":\"" + clave + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("token").asText();
    }

    private long categoriaTortas() {
        Long id = jdbc.queryForObject("SELECT id_categoria FROM categoria WHERE nombre = 'Tortas'", Long.class);
        return id == null ? 0 : id;
    }

    private JsonNode crear(String nombre, boolean disponible) throws Exception {
        String cuerpo = "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"Prueba\",\"categoriaId\":" + categoriaTortas()
                + ",\"precio\":45000.50,\"disponible\":" + disponible + "}";
        String respuesta = mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta);
    }

    @Test
    @DisplayName("Flyway crea la estructura y precarga categorías y usuarios")
    void lasMigracionesCreanLosDatosIniciales() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM categoria", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class)).isEqualTo(2);
        String hash = jdbc.queryForObject("SELECT contrasena_hash FROM usuario WHERE usuario = 'admin'", String.class);
        assertThat(hash).startsWith("$2a$").doesNotContain("Almibar.2026");
    }

    @Test
    void unaContrasenaIncorrectaResponde401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"contrasena\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos."));
    }

    @Test
    @DisplayName("Ciclo completo: crear, consultar, editar, agotar, desactivar y reactivar")
    void cicloCompletoDeUnProducto() throws Exception {
        JsonNode creado = crear("Torta de prueba completa", true);
        long id = creado.get("id").asLong();
        assertThat(creado.get("activo").asBoolean()).isTrue();
        assertThat(creado.get("fechaRegistro").asText()).isNotBlank();

        mvc.perform(get("/api/productos/" + id).header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(45000.50));

        mvc.perform(put("/api/productos/" + id).header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Torta editada\",\"categoriaId\":" + categoriaTortas() + ",\"precio\":50000,\"disponible\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Torta editada"));

        mvc.perform(patch("/api/productos/" + id + "/disponibilidad").header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"disponible\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendible").value(false));

        mvc.perform(delete("/api/productos/" + id).header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // Regla de negocio: inactivo no puede quedar disponible
        mvc.perform(patch("/api/productos/" + id + "/disponibilidad").header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"disponible\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRODUCTO_INACTIVO"));

        mvc.perform(patch("/api/productos/" + id + "/reactivar").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.disponibilidad").value(false));

        // El producto se conserva (eliminación lógica)
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM producto WHERE id_producto = ?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    @DisplayName("La auditoría registra las operaciones exitosas y las rechazadas")
    void auditoriaRegistraExitosasYRechazadas() throws Exception {
        long id = crear("Torta para auditoría", true).get("id").asLong();
        mvc.perform(delete("/api/productos/" + id).header(HttpHeaders.AUTHORIZATION, tokenAdmin)).andExpect(status().isOk());
        mvc.perform(delete("/api/productos/" + id).header(HttpHeaders.AUTHORIZATION, tokenAdmin)).andExpect(status().isConflict());

        java.util.List<AuditoriaProducto> registros = auditoria.findAll().stream().filter(a -> Long.valueOf(id).equals(a.getProductoId())).toList();
        assertThat(registros).extracting(AuditoriaProducto::getResultado)
                .contains(ResultadoOperacion.EXITOSA, ResultadoOperacion.RECHAZADA);
        assertThat(registros).allMatch(a -> "admin".equals(a.getUsuario()));
    }

    @Test
    @DisplayName("MySQL rechaza un producto inactivo y disponible aunque se salte la aplicación")
    void laBaseDeDatosProtegeLaRegla() throws Exception {
        long id = crear("Torta para restricción", true).get("id").asLong();

           // MySQL responde con el error 3819 (restricción CHECK); se verifica que sea la restricción correcta
           assertThatThrownBy(() -> jdbc.update("UPDATE producto SET activo = FALSE, disponibilidad = TRUE WHERE id_producto = ?", id))
                   .isInstanceOf(DataAccessException.class)
                   .hasMessageContaining("chk_producto_inactivo_no_disponible");
           assertThatThrownBy(() -> jdbc.update("UPDATE producto SET precio = 0 WHERE id_producto = ?", id))
                   .isInstanceOf(DataAccessException.class)
                   .hasMessageContaining("chk_producto_precio");
    }

    @Test
    void buscaPorNombreCategoriaYEstado() throws Exception {
        crear("Torta de búsqueda única", false);

        mvc.perform(get("/api/productos").param("nombre", "BÚSQUEDA ÚNICA").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));

        mvc.perform(get("/api/productos").param("nombre", "búsqueda única").param("categoriaId", String.valueOf(categoriaTortas()))
                        .param("activo", "true").param("disponible", "false").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Torta de búsqueda única"));

        mvc.perform(get("/api/productos").param("categoriaId", "9999").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void subeUnaImagenValidaYRechazaUnArchivoQueNoEsImagen() throws Exception {
        long id = crear("Torta con foto", true).get("id").asLong();
        byte[] png = Arrays.copyOf(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, 64);

        mvc.perform(multipart("/api/productos/" + id + "/imagen")
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", png))
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenUrl").value(org.hamcrest.Matchers.startsWith("/uploads/")));

        mvc.perform(multipart("/api/productos/" + id + "/imagen")
                        .file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", "no soy una imagen de verdad".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void elRolConsultaNoPuedeCrear() throws Exception {
        String tokenConsulta = "Bearer " + login("consulta", "Consulta.2026");

        mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, tokenConsulta)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"No\",\"categoriaId\":1,\"precio\":10,\"disponible\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elListadoEsPaginado() throws Exception {
        mvc.perform(get("/api/productos").param("size", "3").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(3))
                .andExpect(jsonPath("$.contenido.length()").value(3));
    }
}
