// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.modelo.PreferenciasProfesor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Preferencias del profesor editables desde la web (persistidas en datos/).
 * Si no hay fichero, se usa el valor por defecto de application.properties.
 */
@Service
public class ServicioPreferenciasProfesor {

    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador = new ObjectMapper();

    public ServicioPreferenciasProfesor(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    public boolean isGeminiGuardarPendientes() {
        return cargar()
                .map(PreferenciasProfesor::isGeminiGuardarPendientes)
                .orElse(propiedades.isGeminiGuardarPendientes());
    }

    public synchronized void setGeminiGuardarPendientes(boolean activo) throws IOException {
        PreferenciasProfesor prefs = cargar().orElse(new PreferenciasProfesor());
        prefs.setGeminiGuardarPendientes(activo);
        guardar(prefs);
    }

    private Optional<PreferenciasProfesor> cargar() {
        Path fichero = ficheroPreferencias();
        if (!Files.isRegularFile(fichero)) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapeador.readValue(fichero.toFile(), PreferenciasProfesor.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void guardar(PreferenciasProfesor prefs) throws IOException {
        Path fichero = ficheroPreferencias();
        Files.createDirectories(fichero.getParent());
        mapeador.writerWithDefaultPrettyPrinter().writeValue(fichero.toFile(), prefs);
    }

    private Path ficheroPreferencias() {
        return Path.of(propiedades.getDirectorioDatos())
                .toAbsolutePath()
                .normalize()
                .resolve("preferencias-profesor.json");
    }
}
