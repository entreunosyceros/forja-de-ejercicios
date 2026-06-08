// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class ServicioEntornoPractica {

    public static final Set<String> MODULOS_CON_ENTORNO_SHELL =
            Set.of("docker", "redes", "sistemas", "git");

    public record ResultadoLimpieza(int elementosEliminados, String mensaje) {}

    private final PropiedadesForjaExamenes propiedades;

    public ServicioEntornoPractica(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    public Path carpetaDatosPractica() {
        return Path.of(propiedades.getRaiz())
                .toAbsolutePath()
                .normalize()
                .resolve("datos-practica");
    }

    public ResultadoLimpieza limpiar() throws IOException {
        Path carpeta = carpetaDatosPractica();
        Files.createDirectories(carpeta);
        int eliminados = 0;
        try (Stream<Path> entradas = Files.list(carpeta)) {
            for (Path entrada : entradas.toList()) {
                String nombre = entrada.getFileName().toString();
                if (".gitkeep".equals(nombre)) {
                    continue;
                }
                if (Files.isDirectory(entrada)) {
                    try (Stream<Path> walk = Files.walk(entrada)) {
                        walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                                // omitir
                            }
                        });
                    }
                } else {
                    Files.deleteIfExists(entrada);
                }
                eliminados++;
            }
        }
        String mensaje = eliminados == 0
                ? "El entorno de práctica ya estaba vacío."
                : "Entorno de práctica limpiado (" + eliminados + " elemento(s)).";
        return new ResultadoLimpieza(eliminados, mensaje);
    }

    public boolean debeLimpiarAlNuevoEjercicio(String modulo) {
        if (modulo == null || modulo.isBlank()) {
            return false;
        }
        if (!propiedades.isLimpiarPracticaAlNuevoEjercicio()) {
            return false;
        }
        return MODULOS_CON_ENTORNO_SHELL.contains(modulo.strip().toLowerCase());
    }
}
