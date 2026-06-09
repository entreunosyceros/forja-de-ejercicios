// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.configuracion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({PropiedadesForjaExamenes.class, PropiedadesLogin.class})
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final PropiedadesForjaExamenes propiedades;

    public ConfiguracionWeb(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        List<String> ubicaciones = new ArrayList<>();
        for (Path carpeta : rutasImagenesPosibles()) {
            if (Files.isDirectory(carpeta)) {
                ubicaciones.add(carpeta.toUri().toString());
                break;
            }
        }
        ubicaciones.add("classpath:/static/img/");

        registro.addResourceHandler("/img/**")
                .addResourceLocations(ubicaciones.toArray(String[]::new));
    }

    private List<Path> rutasImagenesPosibles() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path raiz = Path.of(propiedades.getRaiz()).toAbsolutePath().normalize();
        return List.of(
                cwd.resolve("src/img"),
                cwd.resolve("web/src/img"),
                raiz.resolve("web/src/img"));
    }
}
