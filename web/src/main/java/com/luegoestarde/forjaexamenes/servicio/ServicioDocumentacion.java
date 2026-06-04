package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luegoestarde.forjaexamenes.configuracion.CargadorEnvFichero;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private final ObjectMapper mapeador = new ObjectMapper();

    public ServicioDocumentacion(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    public Path directorioIndice() {
        return Path.of(propiedades.getDirectorioIndice()).toAbsolutePath().normalize();
    }

    public List<ModuloDocumentacion> listarModulosIndexados() {
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

    public List<String> listarIdsModulos() {
        return listarModulosIndexados().stream().map(ModuloDocumentacion::id).toList();
    }

    public List<SeccionDocumentacion> listarSecciones(String moduloId) {
        return listarModulosIndexados().stream()
                .filter(m -> m.id().equals(moduloId))
                .findFirst()
                .map(ModuloDocumentacion::secciones)
                .orElse(List.of());
    }

    public boolean geminiConfigurado() {
        String raiz = Path.of(propiedades.getRaiz()).toAbsolutePath().normalize().toString();
        String clave = CargadorEnvFichero.resolverGeminiApiKey(propiedades.getGeminiApiKey(), raiz);
        return !clave.isBlank();
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
