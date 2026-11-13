package com.reposteria.productos.mapper;

import com.reposteria.productos.dto.response.CategoriaResponse;
import com.reposteria.productos.dto.response.PaginaResponse;
import com.reposteria.productos.dto.response.ProductoResponse;
import com.reposteria.productos.model.entity.Categoria;
import com.reposteria.productos.model.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Convierte entidades en DTO. Las entidades nunca salen hacia la vista,
 * así un cambio en la base de datos no rompe la API.
 */
@Component
public class ProductoMapper {

    public ProductoResponse aRespuesta(Producto producto) {
        return new ProductoResponse(
                producto.getIdProducto(),
                producto.getNombre(),
                producto.getDescripcion(),
                aRespuesta(producto.getCategoria()),
                producto.getPrecio(),
                producto.isDisponibilidad(),
                producto.isActivo(),
                producto.esVendible(),
                producto.getImagenUrl(),
                producto.getFechaRegistro(),
                producto.getFechaActualizacion());
    }

    public CategoriaResponse aRespuesta(Categoria categoria) {
        return new CategoriaResponse(categoria.getIdCategoria(), categoria.getNombre());
    }

    public PaginaResponse<ProductoResponse> aPagina(Page<Producto> pagina) {
        return new PaginaResponse<>(
                pagina.getContent().stream().map(producto -> aRespuesta(producto)).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
