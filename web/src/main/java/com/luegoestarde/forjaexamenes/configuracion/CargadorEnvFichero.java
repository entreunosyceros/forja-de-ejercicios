package com.luegoestarde.forjaexamenes.configuracion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lee variables de {@code examenforge/.env} (misma lógica que Python).
 * Evita depender solo de {@code spring.config.import} o de variables heredadas del shell.
 */
public final class CargadorEnvFichero {

    private static final String FICHERO_ENV = ".env";

    private CargadorEnvFichero() {}

    public static Optional<String> leer(String raizProyecto, String nombreVariable) {
        return Optional.ofNullable(leerTodas(raizProyecto).get(nombreVariable));
    }

    public static Map<String, String> leerTodas(String raizProyecto) {
        Path env = Path.of(raizProyecto).toAbsolutePath().normalize().resolve(FICHERO_ENV);
        if (!Files.isRegularFile(env)) {
            return Map.of();
        }
        Map<String, String> variables = new HashMap<>();
        try {
            for (String linea : Files.readAllLines(env, StandardCharsets.UTF_8)) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#") || !linea.contains("=")) {
                    continue;
                }
                int igual = linea.indexOf('=');
                String clave = linea.substring(0, igual).trim();
                String valor = linea.substring(igual + 1).trim();
                if ((valor.startsWith("\"") && valor.endsWith("\""))
                        || (valor.startsWith("'") && valor.endsWith("'"))) {
                    valor = valor.substring(1, valor.length() - 1);
                }
                if (!clave.isEmpty()) {
                    variables.put(clave, valor);
                }
            }
        } catch (IOException e) {
            return Map.of();
        }
        return variables;
    }

    public static String resolverGeminiApiKey(String desdePropiedades, String raizProyecto) {
        if (desdePropiedades != null && !desdePropiedades.isBlank()) {
            return desdePropiedades.strip();
        }
        Map<String, String> env = leerTodas(raizProyecto);
        String clave = env.getOrDefault("GEMINI_API_KEY", "").strip();
        if (!clave.isEmpty()) {
            return clave;
        }
        return env.getOrDefault("FORJAEXAMENES_GEMINI_API_KEY", "").strip();
    }

    public static String resolverGeminiModelo(String desdePropiedades, String raizProyecto) {
        if (desdePropiedades != null && !desdePropiedades.isBlank()) {
            return desdePropiedades.strip();
        }
        return leer(raizProyecto, "FORJAEXAMENES_GEMINI_MODEL").orElse("").strip();
    }
}
