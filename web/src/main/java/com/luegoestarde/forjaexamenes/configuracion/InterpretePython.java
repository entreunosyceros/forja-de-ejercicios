// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.configuracion;

/**
 * Resuelve el ejecutable de Python (python3 en Linux, python en Windows por defecto).
 */
public final class InterpretePython {

    private InterpretePython() {}

    public static String resolver(PropiedadesForjaExamenes propiedades) {
        if (propiedades == null) {
            return porDefectoSegunSistema();
        }
        String configurado = propiedades.getPythonInterprete();
        if (configurado != null && !configurado.isBlank()) {
            return configurado.trim();
        }
        return porDefectoSegunSistema();
    }

    private static String porDefectoSegunSistema() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "python" : "python3";
    }
}
