// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public class ServicioApuntesPdf {

    private final PropiedadesForjaExamenes propiedades;

    public ServicioApuntesPdf(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    /**
     * Resuelve la ruta del PDF indexado (campo {@code fuente}, p. ej. {@code documentacion/docker/x.pdf}).
     */
    public Path resolverPdf(String fuente) throws IOException {
        if (fuente == null || fuente.isBlank()) {
            throw new IllegalArgumentException("Ruta de apuntes no indicada");
        }
        String normalizada = fuente.replace('\\', '/').trim();
        if (normalizada.contains("..")) {
            throw new IllegalArgumentException("Ruta de apuntes no válida");
        }

        Path raiz = Path.of(propiedades.getRaiz()).toAbsolutePath().normalize();
        Path carpetaDocumentacion = Path.of(propiedades.getDirectorioDocumentacion())
                .toAbsolutePath().normalize();

        Path archivo = raiz.resolve(normalizada).normalize();
        if (!archivo.startsWith(carpetaDocumentacion)) {
            throw new IllegalArgumentException("El PDF debe estar dentro de documentacion/");
        }
        if (!Files.isRegularFile(archivo)) {
            throw new IOException("No se encuentra el PDF: " + archivo);
        }
        if (!archivo.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF");
        }
        return archivo;
    }
}
