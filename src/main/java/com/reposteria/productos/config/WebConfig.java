package com.reposteria.productos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Configuración de la vista: sirve las imágenes guardadas localmente en /uploads/**
 * y abre el inicio de sesión al entrar a la raíz del sitio.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PropiedadesApp propiedades;

    public WebConfig(PropiedadesApp propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        String carpeta = Path.of(propiedades.imagenes().directorio()).toAbsolutePath().normalize().toUri().toString();
        if (!carpeta.endsWith("/")) {
            carpeta = carpeta + "/";
        }
        registro.addResourceHandler("/uploads/**").addResourceLocations(carpeta);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registro) {
        registro.addRedirectViewController("/", "/login.html");
    }
}
