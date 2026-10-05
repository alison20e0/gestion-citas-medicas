package com.citasmedicas.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.citasmedicas.seguridad.InterceptorPermisos;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final InterceptorPermisos interceptorPermisos;
    private final Path directorioEstatico;

    public ConfiguracionWeb(InterceptorPermisos interceptorPermisos,
            @Value("${citas.paginas-estaticas:./static}") String directorioEstatico) {
        this.interceptorPermisos = interceptorPermisos;
        this.directorioEstatico = Path.of(directorioEstatico).toAbsolutePath().normalize();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptorPermisos).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/pagina/**")
                .addResourceLocations(directorioEstatico.toUri().toString())
                .setCachePeriod(0);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/pagina/index.html");
    }
}