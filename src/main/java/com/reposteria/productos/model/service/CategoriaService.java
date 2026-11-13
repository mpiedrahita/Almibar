package com.reposteria.productos.model.service;

import com.reposteria.productos.dto.response.CategoriaResponse;
import com.reposteria.productos.mapper.ProductoMapper;
import com.reposteria.productos.model.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consulta de categorías (RF-P01). Las categorías vienen precargadas. */
@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final ProductoMapper mapper;

    public CategoriaService(CategoriaRepository categorias, ProductoMapper mapper) {
        this.categorias = categorias;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categorias.findAllByOrderByNombreAsc().stream().map(categoria -> mapper.aRespuesta(categoria)).toList();
    }
}
