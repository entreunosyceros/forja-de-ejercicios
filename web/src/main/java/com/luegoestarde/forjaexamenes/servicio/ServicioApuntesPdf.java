package com.luegoestarde.forjaexamenes.servicio;

import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

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

    /**
     * URL para abrir el PDF en el navegador; {@code #page=N} lo interpreta Chrome/Firefox/Edge.
     */
    public String urlVerPdf(String fuente, Integer pagina) {
        String encoded = UriUtils.encodePath(fuente, StandardCharsets.UTF_8);
        StringBuilder url = new StringBuilder("/ejercicio/apuntes/archivo?fuente=").append(encoded);
        if (pagina != null && pagina > 0) {
            url.append("#page=").append(pagina);
        }
        return url.toString();
    }
}
