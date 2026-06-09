// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.configuracion;

/**
 * Normaliza nombres de modelo Gemini (corrige typos frecuentes como {@code gemini-2.5.flash}).
 */
public final class ModeloGemini {

    public static final String POR_DEFECTO = "gemini-2.5-flash";

    private ModeloGemini() {}

    public static String normalizar(String modelo) {
        if (modelo == null || modelo.isBlank()) {
            return POR_DEFECTO;
        }
        String limpio = modelo.strip();
        // Typo habitual: punto en lugar de guion antes de flash/pro/lite
        limpio = limpio.replaceAll(
                "(?i)gemini-(\\d+\\.\\d+)\\.(flash|pro|lite)",
                "gemini-$1-$2");
        return limpio;
    }
}
