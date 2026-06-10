// Desarrollado por entreunosyceros - 2026
package com.luegoestarde.forjaexamenes.servicio;

/**
 * Convierte la salida de generador.py (con traceback) en un mensaje legible para el alumno.
 */
public final class MensajesErrorGenerador {

    private MensajesErrorGenerador() {}

    public static String resumir(String salidaCompleta) {
        if (salidaCompleta == null || salidaCompleta.isBlank()) {
            return "generador.py falló sin mensaje.";
        }
        String salida = salidaCompleta.strip();

        int runtime = salida.lastIndexOf("RuntimeError:");
        if (runtime >= 0) {
            String resto = salida.substring(runtime + "RuntimeError:".length());
            int fin = resto.indexOf("The above exception");
            if (fin < 0) {
                fin = resto.length();
            }
            return resto.substring(0, fin).strip();
        }

        if (salida.contains("UNAVAILABLE") || salida.contains("503")
                || salida.contains("overloaded") || salida.contains("high demand")) {
            return "El servicio de IA de Google (Gemini) está saturado ahora mismo "
                    + "(error 503). No es un fallo de tu configuración. Espera unos minutos y "
                    + "reintenta; mientras tanto usa los módulos sin IA (poo, bd_sql, docker, "
                    + "redes, git…). El profesor puede probar otro modelo en Perfil.";
        }

        if (salida.contains("RESOURCE_EXHAUSTED") || salida.contains("API_KEY_INVALID")) {
            return "Error de la API Gemini. Revisa examenforge/.env (clave y modelo). "
                    + "Detalle en los logs de la consola.";
        }

        String[] lineas = salida.split("\n");
        for (int i = lineas.length - 1; i >= 0; i--) {
            String linea = lineas[i].strip();
            if (!linea.isEmpty() && !linea.startsWith("File ") && !linea.startsWith("Traceback")) {
                return linea.length() > 500 ? linea.substring(0, 500) + "…" : linea;
            }
        }
        return salida.length() > 500 ? salida.substring(0, 500) + "…" : salida;
    }
}
