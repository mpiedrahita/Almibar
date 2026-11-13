package com.reposteria.productos.controller;

import com.reposteria.productos.apoyo.TokensDePrueba;
import com.reposteria.productos.config.PropiedadesApp;
import com.reposteria.productos.dto.response.CategoriaResponse;
import com.reposteria.productos.dto.response.PaginaResponse;
import com.reposteria.productos.dto.response.ProductoResponse;
import com.reposteria.productos.exception.DatosInvalidosException;
import com.reposteria.productos.exception.RecursoNoEncontradoException;
import com.reposteria.productos.exception.ReglaNegocioException;
import com.reposteria.productos.model.rules.ReglasProducto;
import com.reposteria.productos.model.service.ProductoService;
import com.reposteria.productos.security.config.RespuestasSeguridad;
import com.reposteria.productos.security.config.SecurityConfig;
import com.reposteria.productos.security.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la API: códigos HTTP, validaciones, formato de error y seguridad (401 y 403).
 * El servicio se reemplaza por un doble, así se prueba solo el controlador.
 */
@WebMvcTest(ProductoController.class)
@Import({SecurityConfig.class, RespuestasSeguridad.class, JwtService.class})
@EnableConfigurationProperties(PropiedadesApp.class)
@ActiveProfiles("test")
@DisplayName("API de productos")
class ProductoControllerTest {

    private static final String CUERPO_VALIDO = """
            {"nombre":"Torta de zanahoria","descripcion":"Con nueces","categoriaId":5,"precio":45000,"disponible":true}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductoService servicio;

    private static ProductoResponse producto(long id, boolean activo, boolean disponible) {
        return new ProductoResponse(id, "Torta de zanahoria", "Con nueces", new CategoriaResponse(5L, "Tortas"),
                new BigDecimal("45000"), disponible, activo, activo && disponible, null,
                LocalDateTime.of(2026, 11, 1, 9, 0), LocalDateTime.of(2026, 11, 1, 9, 0));
    }

    @Nested
    @DisplayName("Seguridad (RNF-06 y RNF-07)")
    class Seguridad {

        @Test
        void sinTokenResponde401ConElFormatoDeError() throws Exception {
            mvc.perform(get("/api/productos"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.estado").value(401))
                    .andExpect(jsonPath("$.mensaje").exists());
        }

        @Test
        void conTokenVencidoResponde401ExplicandoElMotivo() throws Exception {
            mvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.vencido()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").value(containsString("venció")));
        }

        @Test
        void conTokenAlteradoResponde401() throws Exception {
            mvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.alterado()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void elRolConsultaPuedeVer() throws Exception {
            when(servicio.buscar(any(), any())).thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));

            mvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.consulta()))
                    .andExpect(status().isOk());
        }

        @Test
        void elRolConsultaNoPuedeModificar() throws Exception {
            mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.consulta())
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.estado").value(403));
            verify(servicio, never()).registrar(any());
        }

        @Test
        void desactivarSinTokenResponde401() throws Exception {
            mvc.perform(delete("/api/productos/1")).andExpect(status().isUnauthorized());
        }

        @Test
        void losDisponiblesSonPublicos() throws Exception {
            when(servicio.listarDisponibles(any())).thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));

            mvc.perform(get("/api/productos/disponibles")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Operaciones CRUD")
    class Operaciones {

        @Test
        void registrarResponde201ConLocation() throws Exception {
            when(servicio.registrar(any())).thenReturn(producto(9, true, true));

            mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin())
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/api/productos/9")))
                    .andExpect(jsonPath("$.id").value(9))
                    .andExpect(jsonPath("$.vendible").value(true));
        }

        @Test
        void consultarUnoResponde200() throws Exception {
            when(servicio.obtener(9L)).thenReturn(producto(9, true, false));

            mvc.perform(get("/api/productos/9").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.consulta()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disponibilidad").value(false))
                    .andExpect(jsonPath("$.categoria.nombre").value("Tortas"));
        }

        @Test
        void consultarUnoInexistenteResponde404() throws Exception {
            when(servicio.obtener(404L)).thenThrow(new RecursoNoEncontradoException("No existe un producto con el id 404."));

            mvc.perform(get("/api/productos/404").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.mensaje").value("No existe un producto con el id 404."));
        }

        @Test
        void actualizarResponde200() throws Exception {
            when(servicio.actualizar(eq(9L), any())).thenReturn(producto(9, true, true));

            mvc.perform(put("/api/productos/9").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin())
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isOk());
        }

        @Test
        void desactivarResponde200ConElProductoInactivo() throws Exception {
            when(servicio.desactivar(9L)).thenReturn(producto(9, false, false));

            mvc.perform(delete("/api/productos/9").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activo").value(false));
        }

        @Test
        void reactivarResponde200() throws Exception {
            when(servicio.reactivar(9L)).thenReturn(producto(9, true, false));

            mvc.perform(patch("/api/productos/9/reactivar").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activo").value(true));
        }
    }

    @Nested
    @DisplayName("Validaciones y regla de negocio")
    class Validaciones {

        @Test
        void camposObligatoriosResponden400ConElDetalle() throws Exception {
            mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\",\"precio\":-5}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[*].campo", hasItem("nombre")))
                    .andExpect(jsonPath("$.campos[*].campo", hasItem("precio")))
                    .andExpect(jsonPath("$.campos[*].campo", hasItem("categoriaId")));
            verify(servicio, never()).registrar(any());
        }

        @Test
        void precioConTresDecimalesResponde400() throws Exception {
            mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Torta\",\"categoriaId\":5,\"precio\":1000.555,\"disponible\":true}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[0].campo").value("precio"));
        }

        @Test
        void jsonMalFormadoResponde400() throws Exception {
            mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin())
                            .contentType(MediaType.APPLICATION_JSON).content("{nombre:"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void categoriaInexistenteResponde422() throws Exception {
            when(servicio.registrar(any())).thenThrow(new DatosInvalidosException("categoriaId", "La categoría elegida no existe."));

            mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin())
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.campos[0].campo").value("categoriaId"));
        }

        @Test
        @DisplayName("La regla de negocio responde 409 con su código")
        void reglaDeNegocioResponde409() throws Exception {
            when(servicio.cambiarDisponibilidad(anyLong(), anyBoolean()))
                    .thenThrow(new ReglaNegocioException(ReglasProducto.PRODUCTO_INACTIVO, "«Torta negra» está fuera de carta."));

            mvc.perform(patch("/api/productos/8/disponibilidad").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"disponible\":true}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value(ReglasProducto.PRODUCTO_INACTIVO));
        }

        @Test
        void tamanoDePaginaMayorA100Responde400() throws Exception {
            mvc.perform(get("/api/productos?size=500").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void idQueNoEsNumeroResponde400() throws Exception {
            mvc.perform(get("/api/productos/abc").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unErrorInesperadoResponde500SinDetallesInternos() throws Exception {
            when(servicio.obtener(1L)).thenThrow(new IllegalStateException("SELECT * FROM producto falló en /var/app"));

            mvc.perform(get("/api/productos/1").header(HttpHeaders.AUTHORIZATION, TokensDePrueba.admin()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado. Inténtalo de nuevo."));
        }
    }
}
