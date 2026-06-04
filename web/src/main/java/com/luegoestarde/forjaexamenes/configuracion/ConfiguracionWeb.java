package com.luegoestarde.forjaexamenes.configuracion;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({PropiedadesForjaExamenes.class, PropiedadesLogin.class})
public class ConfiguracionWeb implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        String[] ubicaciones = {"classpath:/static/img/"};

        Path carpetaImagenes = Path.of("src/img").toAbsolutePath().normalize();
        if (Files.isDirectory(carpetaImagenes)) {
            String rutaArchivo = carpetaImagenes.toUri().toString();
            ubicaciones = new String[]{rutaArchivo, ubicaciones[0]};
        }

        registro.addResourceHandler("/img/**").addResourceLocations(ubicaciones);
    }
}
