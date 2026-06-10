// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.luegoestarde.forjaexamenes.modelo.Escenario;
import com.luegoestarde.forjaexamenes.util.MapeadorJson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Exporta e importa ejercicios del banco entre instalaciones independientes
 * (profesor → carpeta compartida → alumno).
 */
@Service
public class ServicioBancoPortable {

    public static final String FORMATO_PAQUETE = "forja-banco-ejercicios";
    public static final int VERSION_PAQUETE = 1;

    public record ResultadoImportacion(
            int importados,
            int actualizados,
            int omitidos,
            List<String> detalles) {}

    private final ServicioBancoEjercicios servicioBanco;
    private final ObjectMapper mapeador;

    public ServicioBancoPortable(ServicioBancoEjercicios servicioBanco) {
        this.servicioBanco = servicioBanco;
        this.mapeador = MapeadorJson.snakeCase();
    }

    public int contarAprobados() throws IOException {
        Path aprobados = servicioBanco.carpetaAprobados();
        if (!Files.isDirectory(aprobados)) {
            return 0;
        }
        try (var stream = Files.walk(aprobados)) {
            return (int) stream.filter(p -> p.toString().endsWith(".json")).count();
        }
    }

    public byte[] exportarPaqueteCompleto() throws IOException {
        List<JsonNode> ejercicios = new ArrayList<>();
        Path aprobados = servicioBanco.carpetaAprobados();
        if (Files.isDirectory(aprobados)) {
            try (var stream = Files.walk(aprobados)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .sorted()
                        .forEach(p -> {
                            try {
                                JsonNode datos = mapeador.readTree(p.toFile());
                                ejercicios.add(limpiarParaBanco(datos));
                            } catch (IOException ignored) {
                                // omitir corruptos
                            }
                        });
            }
        }
        return escribirPaquete(ejercicios);
    }

    public byte[] exportarEjercicio(Escenario escenario) throws IOException {
        JsonNode nodo = mapeador.valueToTree(escenario);
        return escribirPaquete(List.of(limpiarParaBanco(nodo)));
    }

    public synchronized ResultadoImportacion importar(byte[] contenido) throws IOException {
        if (contenido == null || contenido.length == 0) {
            throw new IOException("El fichero está vacío.");
        }
        if (contenido.length > 5 * 1024 * 1024) {
            throw new IOException("El fichero supera el límite de 5 MB.");
        }

        JsonNode raiz = mapeador.readTree(contenido);
        List<JsonNode> ejercicios = extraerEjercicios(raiz);

        int importados = 0;
        int actualizados = 0;
        int omitidos = 0;
        List<String> detalles = new ArrayList<>();

        for (JsonNode ejercicio : ejercicios) {
            try {
                ObjectNode limpio = limpiarParaBanco(ejercicio);
                validarEjercicio(limpio);
                String id = limpio.path("id").asText("");
                Path destino = rutaDestino(limpio);
                limpio.put("modulo", normalizarModuloBanco(
                        limpio.path("modulo").asText(""),
                        destino.getParent().getFileName().toString()));
                boolean existia = Files.isRegularFile(destino);
                Files.createDirectories(destino.getParent());
                mapeador.writerWithDefaultPrettyPrinter().writeValue(destino.toFile(), limpio);
                if (existia) {
                    actualizados++;
                    detalles.add("Actualizado: " + id);
                } else {
                    importados++;
                    detalles.add("Importado: " + id);
                }
            } catch (Exception ex) {
                omitidos++;
                detalles.add("Omitido: " + ex.getMessage());
            }
        }

        if (importados + actualizados == 0) {
            throw new IOException("No se importó ningún ejercicio válido.");
        }

        servicioBanco.reconstruirCatalogo();
        return new ResultadoImportacion(importados, actualizados, omitidos, detalles);
    }

    private byte[] escribirPaquete(List<JsonNode> ejercicios) throws IOException {
        ObjectNode paquete = mapeador.createObjectNode();
        paquete.put("formato", FORMATO_PAQUETE);
        paquete.put("version", VERSION_PAQUETE);
        paquete.put("exportado", Instant.now().toString());
        ArrayNode arr = mapeador.createArrayNode();
        ejercicios.forEach(arr::add);
        paquete.set("ejercicios", arr);
        return mapeador.writerWithDefaultPrettyPrinter().writeValueAsBytes(paquete);
    }

    private List<JsonNode> extraerEjercicios(JsonNode raiz) throws IOException {
        if (raiz == null || raiz.isNull()) {
            throw new IOException("JSON no válido.");
        }
        if (raiz.has("formato") && FORMATO_PAQUETE.equals(raiz.path("formato").asText())) {
            List<JsonNode> lista = new ArrayList<>();
            JsonNode ejercicios = raiz.path("ejercicios");
            if (!ejercicios.isArray() || ejercicios.isEmpty()) {
                throw new IOException("El paquete no contiene ejercicios.");
            }
            ejercicios.forEach(lista::add);
            return lista;
        }
        if (raiz.has("criterios") && raiz.has("enunciado")) {
            return List.of(raiz);
        }
        throw new IOException(
                "Formato no reconocido. Usa un paquete forja-banco-ejercicios o un ejercicio JSON con criterios.");
    }

    private ObjectNode limpiarParaBanco(JsonNode original) {
        ObjectNode nodo = original.deepCopy();
        nodo.remove("usuario");
        nodo.remove("fecha_hora");
        nodo.remove("usuario_acceso");
        nodo.remove("generado_en");
        nodo.remove("dificultad");

        if (!nodo.has("id") || nodo.path("id").asText("").isBlank()) {
            nodo.put("id", "banco-" + UUID.randomUUID().toString().substring(0, 8));
        }

        ObjectNode params;
        if (nodo.path("parametros").isObject()) {
            params = (ObjectNode) nodo.path("parametros");
        } else {
            params = mapeador.createObjectNode();
        }
        if (!params.has("generado_con")) {
            params.put("generado_con", "banco");
        }
        params.put("importado_en", Instant.now().toString());
        nodo.set("parametros", params);
        return nodo;
    }

    private void validarEjercicio(JsonNode ejercicio) throws IOException {
        if (!ejercicio.has("criterios") || !ejercicio.path("criterios").isArray()
                || ejercicio.path("criterios").isEmpty()) {
            throw new IOException("Falta la lista de criterios.");
        }
        if (!ejercicio.has("enunciado") || ejercicio.path("enunciado").asText("").isBlank()) {
            throw new IOException("Falta el enunciado.");
        }
        if (!ejercicio.has("modulo") || ejercicio.path("modulo").asText("").isBlank()) {
            throw new IOException("Falta el módulo.");
        }
    }

    private Path rutaDestino(JsonNode ejercicio) {
        String id = ejercicio.path("id").asText("sin-id").replaceAll("[^a-zA-Z0-9_\\-]", "");
        String modulo = ejercicio.path("modulo").asText("general");
        String sub = subcarpetaDesdeModulo(modulo);
        return servicioBanco.carpetaAprobados().resolve(sub).resolve(id + ".json");
    }

    static String normalizarModuloBanco(String modulo, String subcarpeta) {
        String limpio = modulo != null ? modulo.strip() : "";
        if (limpio.isBlank() || "aprobados".equals(limpio) || "banco_aprobados".equals(limpio)) {
            if (subcarpeta != null && !subcarpeta.isBlank()
                    && !"aprobados".equals(subcarpeta) && !"general".equals(subcarpeta)) {
                return subcarpeta.startsWith("banco_") ? subcarpeta : "banco_" + subcarpeta;
            }
            return "banco_general";
        }
        if (limpio.startsWith("docs_")) {
            return limpio;
        }
        if (limpio.startsWith("banco_") && !"banco_aprobados".equals(limpio)) {
            return limpio;
        }
        return "banco_" + limpio.replace("banco_", "");
    }

    private static String subcarpetaDesdeModulo(String modulo) {
        String limpio = modulo != null ? modulo.strip() : "";
        if (limpio.startsWith("docs_")) {
            return limpio.substring(5);
        }
        String sub = limpio.replace("banco_", "").toLowerCase(Locale.ROOT);
        if (sub.isBlank() || "aprobados".equals(sub)) {
            return "general";
        }
        return sub;
    }
}
