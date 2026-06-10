// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Abre una carpeta en el explorador de archivos del sistema operativo donde se
 * ejecuta la aplicación. Pensado para uso local (el profesor corre la app en su
 * propio PC); en un servidor sin escritorio no habrá explorador disponible.
 */
@Service
public class ServicioExploradorArchivos {

    private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    /** Indica si el sistema parece tener un escritorio donde abrir el explorador. */
    public boolean disponible() {
        if (esWindows() || esMac()) {
            return true;
        }
        // En Linux solo tiene sentido con sesión gráfica (DISPLAY o Wayland).
        return System.getenv("DISPLAY") != null || System.getenv("WAYLAND_DISPLAY") != null;
    }

    /**
     * Abre la carpeta indicada en el explorador. La crea si no existe.
     * @return mensaje descriptivo del resultado.
     */
    public String abrirCarpeta(Path carpeta) throws IOException {
        Path destino = carpeta.toAbsolutePath().normalize();
        Files.createDirectories(destino);

        if (!disponible()) {
            throw new IllegalStateException(
                    "Este equipo no tiene un explorador de archivos disponible (¿servidor sin escritorio?). "
                            + "La carpeta está en: " + destino);
        }

        List<String> comando = comandoSegunSistema(destino.toString());
        if (comando == null) {
            throw new IllegalStateException(
                    "No sé cómo abrir el explorador en este sistema. La carpeta está en: " + destino);
        }

        try {
            new ProcessBuilder(comando).start();
        } catch (IOException e) {
            throw new IOException(
                    "No se pudo abrir el explorador. La carpeta está en: " + destino, e);
        }
        return "Abriendo la carpeta en el explorador: " + destino;
    }

    private List<String> comandoSegunSistema(String ruta) {
        if (esWindows()) {
            return List.of("explorer.exe", ruta);
        }
        if (esMac()) {
            return List.of("open", ruta);
        }
        if (OS.contains("nux") || OS.contains("nix")) {
            return List.of("xdg-open", ruta);
        }
        return null;
    }

    private boolean esWindows() {
        return OS.contains("win");
    }

    private boolean esMac() {
        return OS.contains("mac") || OS.contains("darwin");
    }
}
