package com.reposteria.productos.model.service;

import com.reposteria.productos.dto.response.CategoriaResponse;
import com.reposteria.productos.mapper.ProductoMapper;
import com.reposteria.productos.model.entity.Categoria;
import com.reposteria.productos.model.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoriaServiceTest {

    @Test
    void listaLasCategoriasOrdenadas() {
        CategoriaRepository repositorio = mock(CategoriaRepository.class);
        when(repositorio.findAllByOrderByNombreAsc())
                .thenReturn(List.of(new Categoria(4L, "Galletas y alfajores"), new Categoria(5L, "Tortas")));

        List<CategoriaResponse> categorias = new CategoriaService(repositorio, new ProductoMapper()).listar();

        assertThat(categorias).extracting(CategoriaResponse::nombre).containsExactly("Galletas y alfajores", "Tortas");
    }
}
