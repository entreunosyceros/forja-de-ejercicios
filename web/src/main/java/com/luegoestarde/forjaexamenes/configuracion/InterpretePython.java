// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.configuracion;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resuelve el ejecutable de Python (python3 en Linux, python en Windows por defecto).
 */
public final class InterpretePython {

    private InterpretePython() {}

    public static String resolver(PropiedadesForjaExamenes propiedades) {
        if (propiedades == null) {
            return porDefectoSegunSistema();
        }
        String desdeEnv = CargadorEnvFichero.leer(propiedades.getRaiz(), "FORJAEXAMENES_PYTHON_INTERPRETE")
                .orElse("");
        String desdeProperties = propiedades.getPythonInterprete();
        String candidato = !desdeEnv.isBlank() ? desdeEnv : desdeProperties;
        String validado = validarNormalizar(candidato);
        if (validado != null) {
            return validado;
        }
        return porDefectoSegunSistema();
    }

    static String validarNormalizar(String candidato) {
        if (candidato == null || candidato.isBlank()) {
            return null;
        }
        String limpio = candidato.strip();
        if (rutaWindowsCorrupta(limpio)) {
            return null;
        }
        if (limpio.contains("/") || limpio.contains("\\")) {
            Path ejecutable = Path.of(limpio);
            if (Files.isRegularFile(ejecutable)) {
                return ejecutable.toAbsolutePath().normalize().toString();
            }
            return null;
        }
        return limpio;
    }

    private static boolean rutaWindowsCorrupta(String valor) {
        return valor.matches("^[A-Za-z]:[^/\\\\].*")
                && !valor.contains("/")
                && !valor.contains("\\");
    }

    private static String porDefectoSegunSistema() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "python" : "python3";
    }
}
