// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import com.luegoestarde.forjaexamenes.evento.RecursosActualizadosEvent;
import com.luegoestarde.forjaexamenes.evento.RecursosActualizadosEvent.Tipo;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ServicioDocumentacion {

    public record SeccionDocumentacion(String capitulo, String titulo, int fragmentos) {}

    public record ModuloDocumentacion(
            String id,
            String titulo,
            int fragmentos,
            String carpeta,
            List<SeccionDocumentacion> secciones) {}

    private final PropiedadesForjaExamenes propiedades;
    private final ServicioConfiguracionGemini configuracionGemini;
    private final ObjectMapper mapeador = new ObjectMapper();
    private volatile List<ModuloDocumentacion> cacheModulos = List.of();
    private volatile long cacheMtimeIndice = -1L;

    public ServicioDocumentacion(
            PropiedadesForjaExamenes propiedades,
            ServicioConfiguracionGemini configuracionGemini) {
        this.propiedades = propiedades;
        this.configuracionGemini = configuracionGemini;
    }

    public Path directorioIndice() {
        return Path.of(propiedades.getDirectorioIndice()).toAbsolutePath().normalize();
    }

    public List<ModuloDocumentacion> listarModulosIndexados() {
        long mtimeActual = calcularMtimeIndice();
        if (mtimeActual != cacheMtimeIndice) {
            cacheModulos = cargarModulosDesdeDisco();
            cacheMtimeIndice = mtimeActual;
        }
        return cacheModulos;
    }

    @EventListener
    public void alActualizarRecursos(RecursosActualizadosEvent evento) {
        if (evento.getTipo() == Tipo.INDICE_DOCUMENTACION
                || evento.getTipo() == Tipo.CATALOGO_BANCO) {
            invalidarCache();
        }
    }

    public void invalidarCache() {
        cacheMtimeIndice = -1L;
    }

    public List<String> listarIdsModulos() {
        return listarModulosIndexados().stream().map(ModuloDocumentacion::id).toList();
    }

    public boolean geminiConfigurado() {
        return configuracionGemini.estaConfigurado();
    }

    private List<ModuloDocumentacion> cargarModulosDesdeDisco() {
        Path indice = directorioIndice();
        if (!Files.isDirectory(indice)) {
            return List.of();
        }

        List<ModuloDocumentacion> modulos = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(indice, "docs_*.json")) {
            for (Path archivo : stream) {
                modulos.add(leerModulo(archivo));
            }
        } catch (IOException e) {
            return List.of();
        }
        modulos.sort(Comparator.comparing(ModuloDocumentacion::titulo));
        return modulos;
    }

    private long calcularMtimeIndice() {
        Path indice = directorioIndice();
        if (!Files.isDirectory(indice)) {
            return 0L;
        }
        try (Stream<Path> archivos = Files.list(indice)) {
            return archivos
                    .filter(p -> {
                        String nombre = p.getFileName().toString();
                        return nombre.startsWith("docs_") && nombre.endsWith(".json");
                    })
                    .mapToLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .max()
                    .orElse(0L);
        } catch (IOException e) {
            return 0L;
        }
    }

    private ModuloDocumentacion leerModulo(Path archivo) throws IOException {
        JsonNode raiz = mapeador.readTree(archivo.toFile());
        String id = raiz.path("modulo").asText(archivo.getFileName().toString().replace(".json", ""));
        String titulo = raiz.path("titulo_visible").asText(id);
        int fragmentos = raiz.path("total_fragmentos").asInt(
                raiz.path("chunks").isArray() ? raiz.path("chunks").size() : 0);
        String carpeta = raiz.path("carpeta").asText("");
        List<SeccionDocumentacion> secciones = new ArrayList<>();
        if (raiz.path("secciones").isArray()) {
            for (JsonNode nodo : raiz.path("secciones")) {
                secciones.add(new SeccionDocumentacion(
                        nodo.path("capitulo").asText(""),
                        nodo.path("titulo").asText(""),
                        nodo.path("fragmentos").asInt(0)));
            }
        }
        secciones.sort(Comparator.comparing(SeccionDocumentacion::titulo));
        return new ModuloDocumentacion(id, titulo, fragmentos, carpeta, secciones);
    }
}
