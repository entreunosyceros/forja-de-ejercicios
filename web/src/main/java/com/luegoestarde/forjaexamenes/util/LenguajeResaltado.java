package com.luegoestarde.forjaexamenes.util;

/**
 * Lenguaje highlight.js según el módulo del ejercicio.
 */
public final class LenguajeResaltado {

    private LenguajeResaltado() {
    }

    public static String porModulo(String modulo) {
        if (modulo == null || modulo.isBlank()) {
            return "plaintext";
        }
        String mod = modulo.strip().toLowerCase();
        if (mod.startsWith("bd") || mod.contains("sql")) {
            return "sql";
        }
        if (mod.equals("poo") || mod.contains("java")) {
            return "java";
        }
        if (mod.equals("docker") || mod.equals("git") || mod.equals("redes") || mod.equals("sistemas")) {
            return "bash";
        }
        return "plaintext";
    }
}
