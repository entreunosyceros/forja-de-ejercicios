package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.luegoestarde.forjaexamenes.configuracion.PropiedadesForjaExamenes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class ServicioBancoEjercicios {

    public record EntradaCatalogo(String modulo, String titulo, String id) {}

    public record PendienteBanco(
            String id,
            String modulo,
            String titulo,
            String pregunta,
            String palabrasClave,
            String enunciadoPreview,
            String creado) {}

    private final PropiedadesForjaExamenes propiedades;
    private final ObjectMapper mapeador = new ObjectMapper();

    public ServicioBancoEjercicios(PropiedadesForjaExamenes propiedades) {
        this.propiedades = propiedades;
    }

    @PostConstruct
    void inicializarCatalogo() {
        try {
            reconstruirCatalogo();
        } catch (IOException ignored) {
            // banco aún vacío o rutas no accesibles en arranque de tests
        }
    }

    public Path directorioBanco() {
        return Path.of(propiedades.getDirectorioBanco()).toAbsolutePath().normalize();
    }

    public Path carpetaAprobados() {
        return directorioBanco().resolve("aprobados");
    }

    public Path carpetaPendientes() {
        return directorioBanco().resolve("pendientes");
    }

    public List<EntradaCatalogo> listarParaPortada() throws IOException {
        Path catalogo = directorioBanco().resolve("catalogo.json");
        if (!Files.isRegularFile(catalogo)) {
            reconstruirCatalogo();
        }
        if (!Files.isRegularFile(catalogo)) {
            return List.of();
        }
        JsonNode raiz = mapeador.readTree(catalogo.toFile());
        List<EntradaCatalogo> salida = new ArrayList<>();
        java.util.Set<String> vistos = new java.util.LinkedHashSet<>();
        for (JsonNode ej : raiz.path("ejercicios")) {
            String modulo = ej.path("modulo").asText("");
            if (modulo.isBlank() || vistos.contains(modulo)) {
                continue;
            }
            vistos.add(modulo);
            salida.add(new EntradaCatalogo(
                    modulo,
                    ej.path("titulo").asText(modulo),
                    ej.path("id").asText("")));
        }
        return salida;
    }

    public List<PendienteBanco> listarPendientes() throws IOException {
        Path pendientes = carpetaPendientes();
        Files.createDirectories(pendientes);
        List<PendienteBanco> lista = new ArrayList<>();
        try (Stream<Path> stream = Files.list(pendientes)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .forEach(p -> {
                        try {
                            lista.add(leerPendiente(p));
                        } catch (IOException ignored) {
                            // omitir ficheros corruptos
                        }
                    });
        }
        return lista;
    }

    public Optional<PendienteBanco> obtenerPendiente(String id) throws IOException {
        Path ruta = carpetaPendientes().resolve(id + ".json");
        if (!Files.isRegularFile(ruta)) {
            return Optional.empty();
        }
        return Optional.of(leerPendiente(ruta));
    }

    public void aprobar(String id) throws IOException {
        Path origen = carpetaPendientes().resolve(id + ".json");
        if (!Files.isRegularFile(origen)) {
            throw new IllegalArgumentException("Pendiente no encontrado: " + id);
        }
        JsonNode datos = mapeador.readTree(origen.toFile());
        JsonNode escenario = datos.path("escenario_preview");
        if (escenario.isMissingNode() || !escenario.has("criterios")) {
            throw new IllegalArgumentException("El pendiente no tiene escenario válido");
        }
        String modulo = escenario.path("modulo").asText("general");
        String sub = modulo.startsWith("docs_") ? modulo.substring(5) : modulo.replace("banco_", "");
        Path destinoDir = carpetaAprobados().resolve(sub);
        Files.createDirectories(destinoDir);
        ObjectNode escenarioObj = (ObjectNode) escenario.deepCopy();
        escenarioObj.put("id", id);
        JsonNode params = escenarioObj.path("parametros");
        ObjectNode paramsObj = params.isObject() ? (ObjectNode) params : mapeador.createObjectNode();
        paramsObj.put("aprobado_en", Instant.now().toString());
        escenarioObj.set("parametros", paramsObj);
        Path destino = destinoDir.resolve(id + ".json");
        mapeador.writerWithDefaultPrettyPrinter().writeValue(destino.toFile(), escenarioObj);
        Files.deleteIfExists(origen);
        reconstruirCatalogo();
    }

    public void rechazar(String id) throws IOException {
        Files.deleteIfExists(carpetaPendientes().resolve(id + ".json"));
    }

    public void reconstruirCatalogo() throws IOException {
        Path banco = directorioBanco();
        Files.createDirectories(carpetaAprobados());
        ArrayNode ejercicios = mapeador.createArrayNode();
        Path aprobados = carpetaAprobados();
        if (Files.isDirectory(aprobados)) {
            try (Stream<Path> stream = Files.walk(aprobados)) {
                stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    try {
                        JsonNode datos = mapeador.readTree(p.toFile());
                        ObjectNode entrada = mapeador.createObjectNode();
                        entrada.put("id", datos.path("id").asText(p.getFileName().toString().replace(".json", "")));
                        entrada.put("modulo", datos.path("modulo").asText(""));
                        entrada.put("titulo", datos.path("titulo").asText(p.getFileName().toString()));
                        entrada.put("ruta", banco.relativize(p.toAbsolutePath().normalize()).toString().replace('\\', '/'));
                        entrada.put("origen", datos.path("parametros").path("generado_con").asText("banco"));
                        ejercicios.add(entrada);
                    } catch (IOException ignored) {
                        // omitir
                    }
                });
            }
        }
        ObjectNode catalogo = mapeador.createObjectNode();
        catalogo.put("actualizado", Instant.now().toString());
        catalogo.set("ejercicios", ejercicios);
        mapeador.writerWithDefaultPrettyPrinter()
                .writeValue(banco.resolve("catalogo.json").toFile(), catalogo);
    }

    private PendienteBanco leerPendiente(Path ruta) throws IOException {
        JsonNode datos = mapeador.readTree(ruta.toFile());
        String id = datos.path("id").asText(ruta.getFileName().toString().replace(".json", ""));
        JsonNode preview = datos.path("escenario_preview");
        JsonNode propuesta = datos.path("propuesta");
        if (propuesta.isMissingNode()) {
            propuesta = preview.path("parametros").path("propuesta_gemini");
        }
        String palabras = "";
        if (propuesta.has("palabras_clave")) {
            List<String> pcs = new ArrayList<>();
            propuesta.path("palabras_clave").forEach(n -> pcs.add(n.asText()));
            palabras = String.join(", ", pcs);
        }
        return new PendienteBanco(
                id,
                datos.path("modulo").asText(preview.path("modulo").asText("")),
                preview.path("titulo").asText(""),
                propuesta.path("pregunta").asText(""),
                palabras,
                preview.path("enunciado").asText(""),
                datos.path("creado").asText(""));
    }
}
